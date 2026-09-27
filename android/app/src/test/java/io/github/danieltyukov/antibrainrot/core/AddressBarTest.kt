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

    // After a pause the page is opened again from what the bar showed.
    @Test fun addressUrl() {
        assertEquals("https://reddit.com/r/all", Keys.addressUrl("reddit.com/r/all"))
        assertEquals("https://old.reddit.com", Keys.addressUrl(" old.reddit.com "))
        assertEquals("http://example.com/a", Keys.addressUrl("http://example.com/a"))
        assertEquals("HTTPS://example.com", Keys.addressUrl("HTTPS://example.com"))
    }

    // Descriptions as Firefox 156 reports them.
    @Test fun firefoxDescription() {
        assertEquals("mozilla.org/en-US", Keys.addressFromDescription(" mozilla.org/en-US. Search or enter address"))
        assertEquals("linkedin.com", Keys.addressFromDescription(" linkedin.com. Search or enter address"))
        assertEquals("linkedin.com", Keys.addressFromDescription(" linkedin.com. Zoeken of adres invoeren"))
        assertEquals("duckduckgo.com/?q=hello+world&ia=web", Keys.addressFromDescription(" duckduckgo.com/?q=hello+world&ia=web. Search or enter address"))
        assertEquals("keyword:feet", Keys.forAddressBar(s, Keys.addressFromDescription(" google.com/search?q=feet. Search or enter address"), focused = false, before = null))
        assertNull(Keys.addressFromDescription("Search"))
        assertNull(Keys.addressFromDescription(""))
        assertNull(Keys.addressFromDescription(null))
    }
}
