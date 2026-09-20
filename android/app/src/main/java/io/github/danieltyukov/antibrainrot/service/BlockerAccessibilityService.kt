package io.github.danieltyukov.antibrainrot.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import io.github.danieltyukov.antibrainrot.BuildConfig
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import io.github.danieltyukov.antibrainrot.App
import io.github.danieltyukov.antibrainrot.block.BlockActivity
import io.github.danieltyukov.antibrainrot.core.LocalState
import io.github.danieltyukov.antibrainrot.core.Passes
import io.github.danieltyukov.antibrainrot.core.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

// The heart of the app on Android: sees which window is in front, closes
// short-video feeds inside apps, and puts the block screen over blocked apps.
class BlockerAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var settings: Settings = Settings()
    @Volatile private var local: LocalState = LocalState()
    private var lastBackAt = 0L
    private var consecutiveBacks = 0
    private val lastBlockAt = HashMap<String, Long>()
    private var currentPackage: String = ""
    // Filter-on time not yet written to the history; flushed once a minute.
    private var focusPending = 0

    private val ticker = object : Runnable {
        override fun run() {
            if (settings.focus.enabled) focusPending += TICK_SECONDS
            val flush = if (focusPending >= 60) focusPending.also { focusPending = 0 } else 0
            scope.launch {
                Enforcer.tick(App.instance)
                if (flush > 0) App.instance.local.update { Passes.recordFocus(it, flush) }
                // A pass that just ended: re-check the app in front.
                val pkg = currentPackage
                if (pkg.isNotEmpty() && Enforcer.isBlockedApp(settings, local, pkg)) block(pkg)
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
        handler.post(ticker)
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
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
        if (pkg == packageName) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val front = foregroundPackage() ?: pkg
            if (front != packageName && front !in Enforcer.NEVER_BLOCK && !front.contains("launcher") && !front.contains("inputmethod")) currentPackage = front
        }
        val root = rootInActiveWindow
        if (BuildConfig.DEBUG && event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Log.d(TAG, "window: $pkg class=${event.className} root=${root?.packageName} viewId=${root?.viewIdResourceName}")
        }
        if (root != null && closesFeed(pkg, root)) return
        if (root != null && settings.strictMode && guardsOwnSettings(pkg, root)) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val front = foregroundPackage() ?: pkg
            if (Enforcer.isBlockedApp(settings, local, front)) block(front)
        }
    }

    // Short-video feeds: recognised by view ids the apps use for their
    // vertical video pagers. When one is in front, go back. YouTube Shorts is
    // not a setting, mirroring the extension.
    private fun closesFeed(pkg: String, root: AccessibilityNodeInfo): Boolean {
        val ids = when (pkg) {
            "com.google.android.youtube", "app.revanced.android.youtube", "app.rvx.android.youtube" -> YOUTUBE_SHORTS_IDS
            "com.instagram.android" -> if (settings.focus.enabled && settings.reels.instagram) INSTAGRAM_REELS_IDS else emptyList()
            "com.facebook.katana", "com.facebook.lite" -> if (settings.focus.enabled && settings.reels.facebook) FACEBOOK_REELS_IDS else emptyList()
            "com.snapchat.android" -> if (settings.focus.enabled && settings.reels.snapchatSpotlight) SNAPCHAT_SPOTLIGHT_IDS else emptyList()
            else -> emptyList()
        }
        if (ids.isEmpty()) return false
        val has = { id: String -> root.findAccessibilityNodeInfosByViewId("$pkg:id/$id").isNotEmpty() }
        val found = ids.any(has)
        if (BuildConfig.DEBUG) Log.d(TAG, "feed check $pkg found=$found ids=${ids.filter(has)}")
        // A normal video player or a message thread in front means this is
        // not the feed, whatever else is on screen.
        val negative = when (pkg) {
            "com.google.android.youtube", "app.revanced.android.youtube", "app.rvx.android.youtube" -> YOUTUBE_NOT_SHORTS.any(has)
            "com.instagram.android" -> INSTAGRAM_DM.any(has)
            else -> false
        }
        if (!found || negative) {
            consecutiveBacks = 0
            return false
        }
        val now = System.currentTimeMillis()
        if (now - lastBackAt < 700) return true
        lastBackAt = now
        consecutiveBacks += 1
        scope.launch { App.instance.local.update { Passes.recordFeedClosed(it) } }
        if (consecutiveBacks > 3) {
            // The app keeps reopening the feed: leave it entirely.
            performGlobalAction(GLOBAL_ACTION_HOME)
            consecutiveBacks = 0
            scope.launch { App.instance.local.update { Passes.recordBlock(it, pkg = pkg) } }
        } else {
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
        return true
    }

    // Strict mode: while the filter is on, leave the Settings pages that
    // could disable this app (its App info page, the accessibility page).
    private fun guardsOwnSettings(pkg: String, root: AccessibilityNodeInfo): Boolean {
        if (!settings.focus.enabled) return false
        if (pkg != "com.android.settings" && !pkg.startsWith("com.google.android.settings")) return false
        val label = getString(io.github.danieltyukov.antibrainrot.R.string.app_name)
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

    private fun block(pkg: String) {
        val now = System.currentTimeMillis()
        if (now - (lastBlockAt[pkg] ?: 0L) < 1500) return
        lastBlockAt[pkg] = now
        scope.launch { App.instance.local.update { Passes.recordBlock(it, pkg = pkg) } }
        val intent = Intent(this, BlockActivity::class.java)
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
        // View ids seen in open-source Shorts and Reels blockers (see
        // docs/research/mobile.md). Only fullscreen player surfaces, never the
        // shelves that appear inside the home feed.
        val YOUTUBE_SHORTS_IDS = listOf(
            "reel_player_page_container", "reel_watch_fragment_root", "reel_watch_fragment_container",
            "reel_recycler", "reel_watch_player", "reel_video_player", "reel_progress_bar",
            "reel_player_page", "shorts_container", "shorts_video_list", "shorts_player", "reel_watch", "reel_player",
        )
        val YOUTUBE_NOT_SHORTS = listOf("watch_player", "movie_player", "time_bar", "search_results_editor")
        val INSTAGRAM_REELS_IDS = listOf(
            "clips_viewer_view_pager", "clips_viewer_root", "clips_video_container", "clips_viewer_fragment",
            "clips_player", "clips_timeline", "clips_swipe_refresh_container",
        )
        val INSTAGRAM_DM = listOf("direct_thread_feed", "direct_inbox", "reply_bar_edittext")
        val FACEBOOK_REELS_IDS = listOf("fb_shorts_container", "reels_viewer", "fb_shorts_viewer_fragment", "video_reels_view_pager")
        val SNAPCHAT_SPOTLIGHT_IDS = listOf("spotlight_container", "spotlight_carousel", "spotlight_fullscreen", "spotlight_player", "spotlight_fragment")
    }
}
