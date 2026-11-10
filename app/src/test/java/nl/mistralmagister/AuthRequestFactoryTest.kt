package nl.mistralmagister

import nl.mistralmagister.auth.AuthRequestFactory
import nl.mistralmagister.auth.PkcePair
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRequestFactoryTest {
    @Test
    fun `authorize-url bevat M6LOAPP-parameters`() {
        val request = AuthRequestFactory.create(
            tenantHost = "citadel.magister.net",
            pkce = PkcePair("verifier", "challenge"),
            state = "state123",
            nonce = "nonce456",
        )
        val url = request.authorizeUrl
        assertTrue(url.startsWith("https://accounts.magister.net/connect/authorize?"))
        assertTrue(url.contains("client_id=M6LOAPP"))
        assertTrue(url.contains("redirect_uri=m6loapp%3A%2F%2Foauth2redirect%2F"))
        assertTrue(url.contains("response_type=id_token%20code"))
        assertTrue(url.contains("scope=openid%20profile%20offline_access%20magister.mobile%20magister.ecs"))
        assertTrue(url.contains("code_challenge=challenge"))
        assertTrue(url.contains("code_challenge_method=S256"))
        assertTrue(url.contains("acr_values=tenant%3Acitadel.magister.net"))
        assertTrue(url.contains("state=state123"))
        assertTrue(url.contains("nonce=nonce456"))
        assertTrue(url.contains("prompt=select_account"))
    }

    @Test
    fun `extractCode haalt code uit query en fragment`() {
        assertEquals(
            "abc",
            AuthRequestFactory.extractCode("m6loapp://oauth2redirect/?code=abc&state=s"),
        )
        assertEquals(
            "abc",
            AuthRequestFactory.extractCode("m6loapp://oauth2redirect/#code=abc"),
        )
    }

    @Test
    fun `extractCode negeert andere urls`() {
        assertNull(AuthRequestFactory.extractCode("https://evil.example/?code=abc"))
    }

    @Test
    fun `extractError en state`() {
        assertEquals(
            "access_denied",
            AuthRequestFactory.extractError("m6loapp://oauth2redirect/?error=access_denied"),
        )
        assertEquals(
            "s1",
            AuthRequestFactory.extractState("m6loapp://oauth2redirect/?code=c&state=s1"),
        )
    }

    @Test
    fun `tokenform bevat pkce-verifier`() {
        val form = AuthRequestFactory.tokenForm("code123", "verifier123")
        assertEquals("authorization_code", form["grant_type"])
        assertEquals("code123", form["code"])
        assertEquals("verifier123", form["code_verifier"])
        assertEquals("M6LOAPP", form["client_id"])
    }
}
