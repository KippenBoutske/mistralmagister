package nl.mistralmagister

import kotlinx.serialization.json.Json
import nl.mistralmagister.data.Account
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccountParsingTest {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun `account met Persoon-object zoals Magister echt teruggeeft`() {
        val account = json.decodeFromString(
            Account.serializer(),
            """{"Persoon": {"Id": 424242, "Roepnaam": "Jens", "Tussenvoegsel": null, "Achternaam": "de Vries"}}""",
        )
        assertEquals(424242, account.id)
        assertEquals("Jens de Vries", account.displayName)
    }

    @Test
    fun `account met legacy PersoonId-veld`() {
        val account = json.decodeFromString(
            Account.serializer(),
            """{"PersoonId": 7, "Naam": "Test Leerling"}""",
        )
        assertEquals(7, account.id)
        assertEquals("Test Leerling", account.displayName)
    }

    @Test
    fun `onbekend account-formaat geeft nette nulls`() {
        val account = json.decodeFromString(Account.serializer(), """{"Something": "else"}""")
        assertNull(account.id)
        assertNull(account.displayName)
    }
}
