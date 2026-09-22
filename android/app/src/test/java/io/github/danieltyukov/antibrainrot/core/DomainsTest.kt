package io.github.danieltyukov.antibrainrot.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainsTest {
    @Test fun normalize() {
        assertEquals("example.com", Domains.normalize("https://www.Example.com/x?y=1"))
        assertEquals("sub.example.co.uk", Domains.normalize(" sub.example.co.uk. "))
        assertNull(Domains.normalize("localhost"))
        assertNull(Domains.normalize("127.0.0.1"))
        assertNull(Domains.normalize("not a host"))
        assertEquals(listOf("a.com", "b.org"), Domains.parseList("a.com\nA.com, b.org bad"))
    }

    @Test fun matching() {
        assertTrue(Domains.matches("tiktok.com", "www.tiktok.com"))
        assertTrue(Domains.matches("tiktok.com", "m.tiktok.com."))
        assertFalse(Domains.matches("tiktok.com", "nottiktok.com"))
    }

    @Test fun keywords() {
        assertTrue(Domains.matchesKeyword("www.pornhub.com"))
        assertTrue(Domains.matchesKeyword("xvideos.com"))
        assertFalse(Domains.matchesKeyword("xxxlutz.de"))
        assertFalse(Domains.matchesKeyword("pornic.fr"))
        assertFalse(Domains.matchesKeyword("example.com"))
    }

    @Test fun policy() {
        val policy = DomainPolicy(
            adultList = setOf("badsite.example"),
            adultEnabled = true,
            blockedHosts = Presets.hosts(listOf("tiktok")),
            allowed = listOf("allowed.example"),
        )
        assertTrue(policy.isBlocked("cdn.badsite.example"))
        assertTrue(policy.isBlocked("www.tiktok.com"))
        assertTrue(policy.isBlocked("free-porn.example"))
        assertFalse(policy.isBlocked("news.example"))
        assertFalse(policy.isBlocked("allowed.example"))
        val off = DomainPolicy(setOf("badsite.example"), false, emptyList(), emptyList())
        assertFalse(off.isBlocked("badsite.example"))
    }

    @Test fun safeSearchTargets() {
        assertEquals(SafeSearch.GOOGLE, SafeSearch.searchTarget("www.google.com"))
        assertEquals(SafeSearch.GOOGLE, SafeSearch.searchTarget("google.co.uk"))
        assertEquals(SafeSearch.GOOGLE, SafeSearch.searchTarget("www.google.com.au"))
        assertEquals(SafeSearch.GOOGLE, SafeSearch.searchTarget("google.nl"))
        assertNull(SafeSearch.searchTarget("mail.google.com"))
        assertNull(SafeSearch.searchTarget("docs.google.com"))
        assertNull(SafeSearch.searchTarget("googleapis.com"))
        assertEquals(SafeSearch.BING, SafeSearch.searchTarget("www.bing.com"))
        assertEquals(SafeSearch.DUCKDUCKGO, SafeSearch.searchTarget("duckduckgo.com"))
        assertEquals(SafeSearch.YOUTUBE, SafeSearch.youtubeTarget("m.youtube.com"))
        assertEquals(SafeSearch.YOUTUBE, SafeSearch.youtubeTarget("www.youtube.com"))
        assertNull(SafeSearch.youtubeTarget("www.google.com"))
        assertNull(SafeSearch.searchTarget("www.youtube.com"))
    }

    @Test fun policyRewrites() {
        val on = DomainPolicy(emptySet(), adultEnabled = true, blockedHosts = emptyList(), allowed = emptyList(), safeSearch = true, restrictYouTube = true)
        assertEquals(SafeSearch.GOOGLE, on.rewrite("www.google.com"))
        assertEquals(SafeSearch.GOOGLE, on.rewrite("WWW.GOOGLE.COM."))
        assertEquals(SafeSearch.YOUTUBE, on.rewrite("www.youtube.com"))
        assertNull(on.rewrite("example.com"))
        assertFalse(on.isBlocked("www.google.com"))
        val adultOff = DomainPolicy(emptySet(), adultEnabled = false, blockedHosts = emptyList(), allowed = emptyList(), safeSearch = true, restrictYouTube = true)
        assertNull(adultOff.rewrite("www.google.com"))
        val partsOff = DomainPolicy(emptySet(), adultEnabled = true, blockedHosts = emptyList(), allowed = emptyList(), safeSearch = false, restrictYouTube = false)
        assertNull(partsOff.rewrite("www.google.com"))
        assertNull(partsOff.rewrite("www.youtube.com"))
        val allowed = DomainPolicy(emptySet(), adultEnabled = true, blockedHosts = emptyList(), allowed = listOf("google.com"), safeSearch = true, restrictYouTube = true)
        assertNull(allowed.rewrite("www.google.com"))
    }

    @Test fun presets() {
        assertTrue(Presets.hosts(Presets.DEFAULT_IDS).contains("tiktok.com"))
        assertEquals(Presets.ALL.map { it.id }.distinct().size, Presets.ALL.size)
    }
}
