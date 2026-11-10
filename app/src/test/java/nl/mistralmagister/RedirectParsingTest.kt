package nl.mistralmagister

import nl.mistralmagister.auth.AuthRequestFactory
import org.junit.Assert.assertEquals
import org.junit.Test

class RedirectParsingTest {
    @Test
    fun `code en id_token verdeeld over query en fragment`() {
        val url = "m6loapp://oauth2redirect/?code=abc#id_token=eyJ.x.y&state=s1"
        assertEquals("abc", AuthRequestFactory.extractCode(url))
        assertEquals("s1", AuthRequestFactory.extractState(url))
    }

    @Test
    fun `alles in fragment`() {
        val url = "m6loapp://oauth2redirect/#code=abc&state=s1"
        assertEquals("abc", AuthRequestFactory.extractCode(url))
        assertEquals("s1", AuthRequestFactory.extractState(url))
    }

    @Test
    fun `alles in query`() {
        val url = "m6loapp://oauth2redirect/?code=abc&state=s1"
        assertEquals("abc", AuthRequestFactory.extractCode(url))
        assertEquals("s1", AuthRequestFactory.extractState(url))
    }
}
