package io.github.danieltyukov.antibrainrot.core

// Rule keys: an app's package name, or "site:" plus the rule's host. Passes,
// usage and counters are all keyed this way.
object Keys {
    const val SITE = "site:"
    // A page blocked for a word in its address, keyed by the word.
    const val KEYWORD = "keyword:"

    fun site(host: String): String = SITE + host
    fun isSite(key: String): Boolean = key.startsWith(SITE)
    fun keyword(word: String): String = KEYWORD + word
    fun isKeyword(key: String): Boolean = key.startsWith(KEYWORD)
    fun label(key: String): String = key.removePrefix(SITE).removePrefix(KEYWORD)

    // App stores and package installers, blocked as one by apps.blockInstalls.
    val INSTALLERS = setOf(
        "com.android.vending", "com.google.android.packageinstaller", "com.android.packageinstaller",
        "com.sec.android.app.samsungapps", "com.amazon.venezia", "org.fdroid.fdroid", "com.aurora.store",
        "com.huawei.appmarket", "com.xiaomi.mipicks", "com.oppo.market", "com.heytap.market", "com.vivo.appstore",
    )
    private val INSTALL_BLOCK = Rule("block")
    private val KEYWORD_BLOCK = Rule("block")

    fun isInstaller(key: String): Boolean = key in INSTALLERS

    fun ruleFor(s: Settings, key: String): Rule? = when {
        isKeyword(key) -> if (label(key) in s.sites.keywords) KEYWORD_BLOCK else null
        isSite(key) -> s.sites.rules[label(key)]
        s.apps.blockInstalls && key in INSTALLERS -> s.apps.rules[key] ?: INSTALL_BLOCK
        else -> s.apps.rules[key]
    }

    // The rule host a page host falls under, the most specific one.
    fun siteRuleHost(s: Settings, host: String): String? =
        s.sites.rules.keys.filter { Domains.matches(it, host) }.maxByOrNull { it.length }

    fun blockedHosts(s: Settings): List<String> = s.sites.rules.filterValues { it.mode == "block" }.keys.toList()

    // The rule key for the page a browser's address bar shows:
    // "keyword:<word>" when the address carries a blocked keyword, else
    // "site:<rule host>", or null. While the bar has input focus its text is
    // what is being typed plus the browser's inline completion ("li" reads
    // as linkedin.com), not a page, so the key from before stays in front.
    fun forAddressBar(s: Settings, text: String?, focused: Boolean, before: String?): String? {
        if (focused) return before
        if (text == null) return null
        Keywords.match(s.sites.keywords, text)?.let { return keyword(it) }
        val host = Domains.normalize(text) ?: return null
        return siteRuleHost(s, host)?.let { site(it) }
    }
}
