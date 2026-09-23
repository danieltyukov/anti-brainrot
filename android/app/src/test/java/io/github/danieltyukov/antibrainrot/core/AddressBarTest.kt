package io.github.danieltyukov.antibrainrot.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AddressBarTest {
    private val s = Settings(
        sites = Sites(
            rules = mapOf("linkedin.com" to Rule("block"), "reddit.com" to Rule("timer", 15)),
            keywords = listOf("feet"),
        ),
    )

    @Test fun pageShown() {
        assertEquals("site:linkedin.com", Keys.forAddressBar(s, "linkedin.com", focused = false, before = null))
        assertEquals("site:linkedin.com", Keys.forAddressBar(s, "https://www.linkedin.com/feed/", focused = false, before = null))
        assertEquals("site:reddit.com", Keys.forAddressBar(s, "old.reddit.com/r/all", focused = false, before = null))
        assertEquals("keyword:feet", Keys.forAddressBar(s, "google.com/search?q=feet", focused = false, before = null))
        assertNull(Keys.forAddressBar(s, "example.com", focused = false, before = "site:reddit.com"))
        assertNull(Keys.forAddressBar(s, null, focused = false, before = "site:reddit.com"))
    }

    // Typing "li" shows as linkedin.com with the completion selected. That
    // is not a page, so nothing new is in front until the bar lets go.
    @Test fun typingIsNotAPage() {
        assertNull(Keys.forAddressBar(s, "linkedin.com", focused = true, before = null))
        assertNull(Keys.forAddressBar(s, "linkedin.com/feed/", focused = true, before = null))
        assertNull(Keys.forAddressBar(s, "google.com/search?q=feet", focused = true, before = null))
        assertEquals("site:reddit.com", Keys.forAddressBar(s, "linkedin.com", focused = true, before = "site:reddit.com"))
    }
}
