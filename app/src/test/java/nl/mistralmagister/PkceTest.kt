package nl.mistralmagister

import nl.mistralmagister.auth.Pkce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PkceTest {
    @Test
    fun `verifier gebruikt alleen unreserved karakters`() {
        val verifier = Pkce.generate().verifier
        assertTrue(verifier.length == 64)
        assertTrue(verifier.all { it in "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~" })
    }

    @Test
    fun `challenge is base64url zonder padding`() {
        val challenge = Pkce.challenge("test-verifier")
        assertTrue(!challenge.contains('+') && !challenge.contains('/') && !challenge.contains("="))
        assertEquals(43, challenge.length)
    }

    @Test
    fun `bekende vector`() {
        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            Pkce.challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"),
        )
    }
}
