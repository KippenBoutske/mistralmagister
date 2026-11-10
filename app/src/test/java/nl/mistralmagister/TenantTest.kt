package nl.mistralmagister

import nl.mistralmagister.api.KnownSchools
import nl.mistralmagister.api.Tenant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TenantTest {
    @Test
    fun `normalize voegt domein toe`() {
        assertEquals("citadel.magister.net", Tenant.normalize("citadel"))
        assertEquals("citadel.magister.net", Tenant.normalize("https://Citadel.magister.net/"))
    }

    @Test
    fun `citadelcollege-alias wordt gecorrigeerd`() {
        assertEquals("citadel.magister.net", Tenant.normalize("citadelcollege.magister.net"))
    }

    @Test
    fun `looksLikeHost`() {
        assertTrue(Tenant.looksLikeHost("citadel.magister.net"))
        assertEquals(false, Tenant.looksLikeHost(" Citadel College"))
    }

    @Test
    fun `schoolzoekmachine vindt host-invoer`() {
        val hits = KnownSchools.search("kandinsky.magister.net")
        assertEquals("kandinsky.magister.net", hits.single().host)
    }
}
