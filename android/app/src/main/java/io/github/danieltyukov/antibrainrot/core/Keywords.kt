package io.github.danieltyukov.antibrainrot.core

import java.net.URLDecoder

// Blocked keywords: words that put the block screen over any page whose
// address carries one, as a whole word, in the path or query. The host does
// not count, that is the adult list's job. Mirrors the extension's
// lib/keywords.js.
object Keywords {
    const val MAX = 200
    const val MAX_LENGTH = 40
    private val NOT_WORD = Regex("[^\\p{L}\\p{N} ]")
    private val SPACES = Regex("\\s+")
    private val SCHEME = Regex("^[a-z][a-z0-9+.-]*://", RegexOption.IGNORE_CASE)

    fun normalize(entry: String?): String? {
        if (entry == null) return null
        val s = entry.lowercase().replace(NOT_WORD, "").replace(SPACES, " ").trim().take(MAX_LENGTH).trim()
        if (s.length < 2 || s.none { it.isLetterOrDigit() }) return null
        return s
    }

    fun parseList(text: String): List<String> =
        text.split(Regex("[\\n,]+")).mapNotNull { normalize(it) }.distinct().take(MAX)

    // The path and query of an address, decoded, plus signs as spaces the
    // way search engines send them. Takes what a browser's address bar
    // shows, with or without the scheme. Null when there is only a host.
    fun searchable(address: String): String? {
        var s = address.trim()
        if (s.isEmpty()) return null
        s = s.replace(SCHEME, "")
        val slash = s.indexOf('/')
        val query = s.indexOf('?')
        val start = when {
            slash >= 0 && (query < 0 || slash < query) -> slash
            query >= 0 -> query
            else -> return null
        }
        val raw = s.substring(start).substringBefore('#').replace('+', ' ')
        return try {
            URLDecoder.decode(raw, "UTF-8")
        } catch (e: Exception) {
            raw
        }
    }

    private fun regexFor(keyword: String): Regex {
        val words = keyword.split(' ').joinToString("[\\s_.-]+") { Regex.escape(it) }
        return Regex("(?:^|[^\\p{L}\\p{N}])$words(?:$|[^\\p{L}\\p{N}])", RegexOption.IGNORE_CASE)
    }

    // The first listed keyword found in the address, or null.
    fun match(list: List<String>, address: String): String? {
        if (list.isEmpty()) return null
        val text = searchable(address) ?: return null
        for (keyword in list) {
            val k = normalize(keyword) ?: continue
            if (regexFor(k).containsMatchIn(text)) return k
        }
        return null
    }
}
