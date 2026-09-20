package io.github.danieltyukov.antibrainrot.core

data class Preset(val id: String, val label: String, val hosts: List<String>)

// Distracting sites, at host level: the DNS filter cannot see paths, so
// "feed only" presets from the extension become whole-host presets here.
object Presets {
    val ALL = listOf(
        Preset("tiktok", "TikTok", listOf("tiktok.com", "tiktokcdn.com", "tiktokv.com")),
        Preset("instagram", "Instagram", listOf("instagram.com", "cdninstagram.com")),
        Preset("x", "X (Twitter)", listOf("x.com", "twitter.com", "twimg.com")),
        Preset("reddit", "Reddit", listOf("reddit.com", "redd.it", "redditmedia.com", "redditstatic.com")),
        Preset("facebook", "Facebook", listOf("facebook.com", "fb.com", "fbcdn.net")),
        Preset("threads", "Threads", listOf("threads.net", "threads.com")),
        Preset("linkedin", "LinkedIn", listOf("linkedin.com", "licdn.com")),
        Preset("bluesky", "Bluesky", listOf("bsky.app", "bsky.social")),
        Preset("tumblr", "Tumblr", listOf("tumblr.com")),
        Preset("pinterest", "Pinterest", listOf("pinterest.com", "pinimg.com", "pinterest.co.uk", "pinterest.de", "pinterest.fr")),
        Preset("twitch", "Twitch", listOf("twitch.tv", "ttvnw.net")),
        Preset("kick", "Kick", listOf("kick.com")),
        Preset("9gag", "9GAG", listOf("9gag.com")),
        Preset("imgur", "Imgur", listOf("imgur.com")),
        Preset("snapchat", "Snapchat web", listOf("snapchat.com")),
        Preset("netflix", "Netflix", listOf("netflix.com", "nflxvideo.net")),
    )
    val DEFAULT_IDS = listOf("tiktok", "instagram", "x", "reddit", "facebook", "threads", "9gag")

    fun hosts(ids: List<String>): List<String> =
        ALL.filter { it.id in ids }.flatMap { it.hosts }.distinct()

    // The preset a rule host belongs to, for its label.
    fun byHost(host: String): Preset? = ALL.firstOrNull { p -> p.hosts.any { Domains.matches(it, host) } }
}

object Domains {
    private val LABEL = Regex("^[a-z0-9-]+$")

    fun normalize(entry: String?): String? {
        if (entry == null) return null
        var s = entry.trim().lowercase()
        if (s.isEmpty()) return null
        s = s.replace(Regex("^[a-z][a-z0-9+.-]*://"), "")
        s = s.substringBefore('/').substringBefore('?').substringBefore('#').substringBefore(':')
        s = s.removePrefix("www.").trimEnd('.')
        if (!s.contains('.')) return null
        if (s.matches(Regex("^[\\d.]+$"))) return null
        val labels = s.split('.')
        if (labels.any { it.isEmpty() || !LABEL.matches(it) || it.startsWith("-") || it.endsWith("-") }) return null
        return s
    }

    fun parseList(text: String): List<String> =
        text.split(Regex("[\\s,]+")).mapNotNull { normalize(it) }.distinct()

    // host equals the pattern or is a subdomain of it
    fun matches(patternHost: String, host: String): Boolean {
        val h = host.lowercase().removePrefix("www.").trimEnd('.')
        return h == patternHost || h.endsWith(".$patternHost")
    }

    // Hostname keywords from the extension's adult ruleset, unambiguous only.
    val ADULT_KEYWORDS = listOf(
        "porn", "xxx", "xvideos", "xnxx", "xhamster", "hentai", "redtube", "brazzers", "chaturbate",
        "stripchat", "livejasmin", "bongacams", "onlyfans", "fansly", "rule34", "spankbang", "camsoda",
        "myfreecams", "motherless", "fapello", "thothub", "tnaflix", "xmoviesforyou", "jerkmate",
    )
    // Benign hosts the keywords would catch by mistake, mirrored from the extension.
    val ADULT_KEYWORD_EXCEPTIONS = listOf(
        "xxxlutz.de", "xxxlutz.at", "xxxlutz.com", "xxxlutz.ch", "mixxx.org", "xxxpowersports.com",
        "pornic.fr", "odpornosc.org.pl", "bistrotdeshalles-pornic.fr", "faiencerie-pornic.fr",
    )

    fun matchesKeyword(host: String): Boolean {
        val h = host.lowercase()
        if (ADULT_KEYWORD_EXCEPTIONS.any { matches(it, h) }) return false
        val labels = h.split('.')
        return ADULT_KEYWORDS.any { kw ->
            when (kw) {
                "beeg", "cam4" -> labels.any { it == kw }
                else -> h.contains(kw)
            }
        }
    }
}

// Decides whether a DNS name is blocked. Built once per settings change.
class DomainPolicy(
    private val adultList: Set<String>,
    private val adultEnabled: Boolean,
    private val blockedHosts: List<String>,
    private val allowed: List<String>,
) {
    fun isBlocked(host: String): Boolean {
        val h = host.lowercase().trimEnd('.')
        if (allowed.any { Domains.matches(it, h) }) return false
        if (blockedHosts.any { Domains.matches(it, h) }) return true
        if (adultEnabled) {
            if (Domains.matchesKeyword(h)) return true
            var probe = h
            while (probe.contains('.')) {
                if (probe in adultList) return true
                probe = probe.substringAfter('.')
            }
        }
        return false
    }
}
