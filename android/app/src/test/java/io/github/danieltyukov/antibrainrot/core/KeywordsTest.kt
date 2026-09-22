package io.github.danieltyukov.antibrainrot.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeywordsTest {
    @Test fun normalize() {
        assertEquals("feet", Keywords.normalize("  Feet "))
        assertEquals("foot fetish", Keywords.normalize("Foot   Fetish"))
        assertEquals("nudes", Keywords.normalize("nu\"des!"))
        assertNull(Keywords.normalize("a"))
        assertNull(Keywords.normalize(""))
        assertNull(Keywords.normalize("   "))
        assertNull(Keywords.normalize(null))
        assertEquals("x".repeat(40), Keywords.normalize("x".repeat(50)))
        assertEquals(listOf("feet", "nudes", "foot fetish"), Keywords.parseList("feet\nFeet, nudes\n\nfoot fetish"))
        assertEquals(200, Keywords.parseList((0 until 250).joinToString("\n") { "word$it" }).size)
    }

    @Test fun matchesWholeWordsInPathAndQuery() {
        val list = listOf("feet", "foot fetish", "nudes")
        assertEquals("feet", Keywords.match(list, "https://www.google.com/search?q=feet+pics"))
        assertEquals("feet", Keywords.match(list, "google.com/search?q=FEET"))
        assertEquals("feet", Keywords.match(list, "reddit.com/r/feet/"))
        assertEquals("foot fetish", Keywords.match(list, "https://m.youtube.com/results?search_query=foot+fetish"))
        assertEquals("foot fetish", Keywords.match(list, "x.com/search?q=foot%20fetish"))
        assertEquals("foot fetish", Keywords.match(list, "reddit.com/r/foot_fetish/"))
        assertNull(Keywords.match(list, "x.com/search?q=barefeet"))
        assertNull(Keywords.match(list, "x.com/search?q=feetwear"))
        assertNull(Keywords.match(list, "feet.example"))
        assertNull(Keywords.match(list, "https://feet.example/"))
        assertNull(Keywords.match(list, "x.com/"))
        assertNull(Keywords.match(emptyList(), "x.com/?q=feet"))
        assertNull(Keywords.match(list, "x.com/?q=%E0%A4%A"))
    }

    @Test fun keys() {
        val s = Settings(sites = Sites(keywords = listOf("feet")))
        assertEquals(Rule("block"), Keys.ruleFor(s, Keys.keyword("feet")))
        assertNull(Keys.ruleFor(s, Keys.keyword("hands")))
        assertEquals("feet", Keys.label(Keys.keyword("feet")))
        assertEquals(true, Keys.isKeyword("keyword:feet"))
        assertEquals(false, Keys.isSite("keyword:feet"))
    }
}
