package io.github.danieltyukov.antibrainrot.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.core.content.ContextCompat
import io.github.danieltyukov.antibrainrot.App
import io.github.danieltyukov.antibrainrot.BuildConfig
import io.github.danieltyukov.antibrainrot.R
import io.github.danieltyukov.antibrainrot.block.BlockActivity
import io.github.danieltyukov.antibrainrot.core.Domains
import io.github.danieltyukov.antibrainrot.core.Keys
import io.github.danieltyukov.antibrainrot.core.LocalState
import io.github.danieltyukov.antibrainrot.core.Passes
import io.github.danieltyukov.antibrainrot.core.Rule
import io.github.danieltyukov.antibrainrot.core.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

// The heart of the app on Android: sees which app is in front, reads the
// address bar of browsers, puts the block screen over blocked apps and
// sites, meters time in timed ones, and guards the Settings pages in strict
// mode.
class BlockerAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var settings: Settings = Settings()
    @Volatile private var local: LocalState = LocalState()
    private var lastBackAt = 0L
    private val lastBlockAt = HashMap<String, Long>()
    private var currentPackage: String = ""
    // Filter-on time not yet written to the history; flushed once a minute.
    private var focusPending = 0
    // Usage metering: the timed app and timed site in front, and since when.
    private var meteredKeys: Set<String> = emptySet()
    private var meteredSince = 0L
    // The rule site shown in the browser in front, if any, and the last scan.
    private var siteInFront: String? = null
    private var lastUrlScanAt = 0L
    private val power by lazy { getSystemService(Context.POWER_SERVICE) as PowerManager }
    private val notifier by lazy { SessionNotifier(this) }

    // Screen off stops the meter; screen on starts it again for whatever is in front.
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> meter(null)
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> meter(foregroundPackage())
            }
        }
    }

    // A newly installed app, while installs are blocked, gets a Block rule
    // before it is ever opened. Reinstalls keep whatever rule they had.
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != Intent.ACTION_PACKAGE_ADDED) return
            if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return
            val pkg = intent.data?.schemeSpecificPart ?: return
            val s = settings
            if (!s.focus.enabled || !s.apps.blockInstalls || pkg == packageName || pkg in s.apps.rules) return
            scope.launch {
                App.instance.settings.patch { it.copy(apps = it.apps.copy(rules = it.apps.rules + (pkg to Rule("block")))) }
            }
        }
    }

    private val ticker = object : Runnable {
        override fun run() {
            if (settings.focus.enabled) focusPending += TICK_SECONDS
            val flush = if (focusPending >= 60) focusPending.also { focusPending = 0 } else 0
            meter(foregroundPackage())
            notifier.update(settings, local, meteredKeys) { key -> labelFor(key) }
            scope.launch {
                Enforcer.tick(App.instance)
                if (flush > 0) App.instance.local.update { Passes.recordFocus(it, flush) }
                // A session that just ended: re-check the app or site in front.
                val pkg = currentPackage
                val site = siteInFront
                if (pkg.isNotEmpty() && Enforcer.isBlocked(settings, local, pkg)) block(pkg, pkg)
                else if (pkg.isNotEmpty() && site != null && pkg in URL_BARS && Enforcer.isBlocked(settings, local, site)) block(site, pkg)
            }
            handler.postDelayed(this, TICK_SECONDS * 1000L)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        val app = App.instance
        scope.launch { app.settings.flow.collect { settings = it; Enforcer.syncSiteFilter(this@BlockerAccessibilityService, it) } }
        scope.launch { app.local.flow.collect { local = it } }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(this, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        val packages = IntentFilter(Intent.ACTION_PACKAGE_ADDED).apply { addDataScheme("package") }
        ContextCompat.registerReceiver(this, packageReceiver, packages, ContextCompat.RECEIVER_NOT_EXPORTED)
        meteredSince = System.currentTimeMillis()
        handler.post(ticker)
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        try { unregisterReceiver(screenReceiver) } catch (e: Exception) { }
        try { unregisterReceiver(packageReceiver) } catch (e: Exception) { }
        scope.cancel()
        instance = null
        super.onDestroy()
    }

    override fun onInterrupt() {}

    // The app in front is the focused (else the active) application window,
    // not the package of whatever window raised the event: keyboards, system
    // dialogs and overlays raise events too.
    private fun foregroundPackage(): String? {
        val list = try { windows } catch (e: Exception) { null } ?: return null
        val app = list.firstOrNull { it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused }
            ?: list.firstOrNull { it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isActive }
        return app?.root?.packageName?.toString()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val front = foregroundPackage() ?: pkg
            meter(front)
            if (front != packageName && front !in Enforcer.NEVER_BLOCK && !front.contains("launcher") && !front.contains("inputmethod")) currentPackage = front
        }
        if (pkg == packageName) return
        val root = rootInActiveWindow
        if (BuildConfig.DEBUG && event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Log.d(TAG, "window: $pkg class=${event.className} root=${root?.packageName}")
        }
        if (root != null && settings.strictMode && guardsOwnSettings(pkg, root)) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val front = foregroundPackage() ?: pkg
            if (Enforcer.isBlocked(settings, local, front)) {
                block(front, front)
                return
            }
        }
        if (root != null && pkg in URL_BARS && settings.sites.rules.isNotEmpty()) watchAddressBar(pkg, root)
    }

    // Reads the browser's address bar and applies the site's rule. Content
    // events come in bursts, so scans are throttled.
    private fun watchAddressBar(pkg: String, root: AccessibilityNodeInfo) {
        val now = System.currentTimeMillis()
        if (now - lastUrlScanAt < 400) return
        lastUrlScanAt = now
        val key = siteKeyInFront(pkg, root)
        if (key != siteInFront) {
            siteInFront = key
            meter(foregroundPackage() ?: pkg)
        }
        if (key != null && Enforcer.isBlocked(settings, local, key)) block(key, pkg)
    }

    // "site:<rule host>" for the page shown in a known browser, or null.
    private fun siteKeyInFront(pkg: String, root: AccessibilityNodeInfo): String? {
        val id = URL_BARS[pkg] ?: return null
        val node = root.findAccessibilityNodeInfosByViewId("$pkg:id/$id").firstOrNull() ?: return null
        val text = node.text?.toString() ?: return null
        val host = Domains.normalize(text) ?: return null
        val ruleHost = Keys.siteRuleHost(settings, host) ?: return null
        return Keys.site(ruleHost)
    }

    // The rule keys to meter for the app in front: the app itself when it
    // is timed, and the timed site shown in a browser. Both count at once.
    private fun meterKeys(front: String?): Set<String> {
        if (front == null || !power.isInteractive) return emptySet()
        val keys = HashSet<String>()
        if (Enforcer.isTimed(settings, front)) keys.add(front)
        if (front in URL_BARS) siteInFront?.let { if (Enforcer.isTimed(settings, it)) keys.add(it) }
        return keys
    }

    // Charges the metered keys for the whole seconds since the last charge,
    // then meters whatever is in front now. Whole seconds only, and the
    // remainder carries over, so frequent calls do not lose time.
    private fun meter(front: String?) {
        val now = System.currentTimeMillis()
        val keys = meteredKeys
        if (keys.isNotEmpty()) {
            val seconds = ((now - meteredSince) / 1000).toInt()
            if (seconds > 0) {
                meteredSince += seconds * 1000L
                val s = settings
                val pkg = front
                scope.launch {
                    var next = local
                    for (key in keys) next = App.instance.local.update { Passes.recordUsage(it, key, seconds) }
                    // A daily limit may have just run out.
                    if (pkg != null) {
                        val out = keys.firstOrNull { it in meteredKeys && Enforcer.isBlocked(s, next, it) }
                        if (out != null) block(out, pkg)
                    }
                }
            }
        }
        val next = meterKeys(front)
        if (next != keys) {
            meteredKeys = next
            meteredSince = now
            if (next.isEmpty()) notifier.clear()
        }
    }

    private fun labelFor(key: String): String =
        if (Keys.isSite(key)) Keys.label(key) else try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(key, 0)).toString()
        } catch (e: Exception) { key }

    // Strict mode: while the filter is on, leave the Settings pages that
    // could disable this app (its App info page, the accessibility page).
    private fun guardsOwnSettings(pkg: String, root: AccessibilityNodeInfo): Boolean {
        if (!settings.focus.enabled) return false
        val guarded = pkg == "com.android.settings" || pkg.startsWith("com.google.android.settings") || pkg in UNINSTALLERS
        if (!guarded) return false
        val label = getString(R.string.app_name)
        // findAccessibilityNodeInfosByText finds nothing inside Compose
        // screens (the App info page is one), so walk the tree ourselves.
        val hit = root.findAccessibilityNodeInfosByText(label).isNotEmpty() || treeHasText(root, label)
        if (BuildConfig.DEBUG) Log.d(TAG, "settings guard hit=$hit")
        if (!hit) return false
        val now = System.currentTimeMillis()
        if (now - lastBackAt < 700) return true
        lastBackAt = now
        performGlobalAction(GLOBAL_ACTION_BACK)
        return true
    }

    private fun treeHasText(root: AccessibilityNodeInfo, needle: String, limit: Int = 600): Boolean {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var seen = 0
        while (queue.isNotEmpty() && seen < limit) {
            val node = queue.removeFirst()
            seen += 1
            if (node.text?.contains(needle, ignoreCase = true) == true) return true
            if (node.contentDescription?.contains(needle, ignoreCase = true) == true) return true
            for (i in 0 until node.childCount) node.getChild(i)?.let { queue.add(it) }
        }
        return false
    }

    // Shows the block screen for a rule key over the app `pkg` (the browser,
    // for a site).
    private fun block(key: String, pkg: String) {
        val now = System.currentTimeMillis()
        if (now - (lastBlockAt[key] ?: 0L) < 1500) return
        lastBlockAt[key] = now
        scope.launch { App.instance.local.update { Passes.recordBlock(it, pkg = key) } }
        val intent = Intent(this, BlockActivity::class.java)
            .putExtra(BlockActivity.EXTRA_KEY, key)
            .putExtra(BlockActivity.EXTRA_PACKAGE, pkg)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        try {
            startActivity(intent)
        } catch (e: Exception) {
            // Without the overlay permission Android may refuse; fall back to home.
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    companion object {
        private const val TAG = "abr-a11y"
        private const val TICK_SECONDS = 15
        @Volatile var instance: BlockerAccessibilityService? = null

        // The uninstall confirmation lives here; strict mode leaves it when it names this app.
        val UNINSTALLERS = setOf("com.google.android.packageinstaller", "com.android.packageinstaller")

        // Browsers whose address bar can be read, and the view id of that bar.
        val URL_BARS = mapOf(
            "com.android.chrome" to "url_bar",
            "com.chrome.beta" to "url_bar",
            "com.chrome.dev" to "url_bar",
            "com.chrome.canary" to "url_bar",
            "com.brave.browser" to "url_bar",
            "com.vivaldi.browser" to "url_bar",
            "com.kiwibrowser.browser" to "url_bar",
            "com.microsoft.emmx" to "url_bar",
            "org.mozilla.firefox" to "mozac_browser_toolbar_url_view",
            "org.mozilla.firefox_beta" to "mozac_browser_toolbar_url_view",
            "org.mozilla.fenix" to "mozac_browser_toolbar_url_view",
            "org.mozilla.focus" to "display_url",
            "com.sec.android.app.sbrowser" to "location_bar_edit_text",
            "com.opera.browser" to "url_field",
            "com.opera.mini.native" to "url_field",
            "com.duckduckgo.mobile.android" to "omnibarTextInput",
        )
    }
}
