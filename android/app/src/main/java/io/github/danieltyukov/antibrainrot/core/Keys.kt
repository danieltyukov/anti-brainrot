package io.github.danieltyukov.antibrainrot.core

// Rule keys: an app's package name, or "site:" plus the rule's host. Passes,
// usage and counters are all keyed this way.
object Keys {
    const val SITE = "site:"

    fun site(host: String): String = SITE + host
    fun isSite(key: String): Boolean = key.startsWith(SITE)
    fun label(key: String): String = key.removePrefix(SITE)

    fun ruleFor(s: Settings, key: String): Rule? =
        if (isSite(key)) s.sites.rules[label(key)] else s.apps.rules[key]

    // The rule host a page host falls under, the most specific one.
    fun siteRuleHost(s: Settings, host: String): String? =
        s.sites.rules.keys.filter { Domains.matches(it, host) }.maxByOrNull { it.length }

    fun blockedHosts(s: Settings): List<String> = s.sites.rules.filterValues { it.mode == "block" }.keys.toList()
}
