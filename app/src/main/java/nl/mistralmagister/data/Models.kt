package nl.mistralmagister.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Serializable
data class AccountPersoon(
    @SerialName("Id") val id: Int? = null,
    @SerialName("Roepnaam") val roepnaam: String? = null,
    @SerialName("Tussenvoegsel") val tussenvoegsel: String? = null,
    @SerialName("Achternaam") val achternaam: String? = null,
)

@Serializable
data class Account(
    @SerialName("Persoon") val persoon: AccountPersoon? = null,
    @SerialName("PersoonId") val persoonId: Int? = null,
    @SerialName("PersonId") val personId: Int? = null,
    @SerialName("Naam") val naam: String? = null,
) {
    val id: Int? get() = persoon?.id ?: persoonId ?: personId
    val displayName: String?
        get() = listOfNotNull(
            persoon?.roepnaam,
            persoon?.tussenvoegsel,
            persoon?.achternaam,
        ).joinToString(" ").ifBlank { naam }
}

@Serializable
data class Vak(
    @SerialName("Omschrijving") val omschrijving: String? = null,
    @SerialName("Code") val code: String? = null,
)

@Serializable
data class Appointment(
    @SerialName("Id") val id: Int = 0,
    @SerialName("Start") val start: String = "",
    @SerialName("Eind") val eind: String = "",
    @SerialName("LesuurVan") val lesuurVan: Int? = null,
    @SerialName("LesuurTotMet") val lesuurTot: Int? = null,
    @SerialName("Lesuur") val lesuur: Int? = null,
    @SerialName("Locatie") val locatie: String? = null,
    @SerialName("Omschrijving") val omschrijving: String? = null,
    @SerialName("Inhoud") val inhoud: String? = null,
    @SerialName("Status") val status: Int? = null,
    @SerialName("Vak") val vak: Vak? = null,
    @SerialName("AfgelastingInfo") val afgelastingInfo: String? = null,
) {
    val vakNaam: String get() = vak?.omschrijving ?: omschrijving.orEmpty()
    val vakCode: String get() = vak?.code.orEmpty()
    val lesuurLabel: String get() = (lesuurVan ?: lesuur)?.toString() ?: ""
}

@Serializable
data class AppointmentResponse(
    @SerialName("Items") val items: List<Appointment> = emptyList(),
)

@Serializable
data class GradeColumn(
    @SerialName("Naam") val naam: String? = null,
    @SerialName("Omschrijving") val omschrijving: String? = null,
)

@Serializable
data class Grade(
    @SerialName("CijferId") val cijferId: Int = 0,
    @SerialName("CijferStr") val cijferStr: String? = null,
    @SerialName("Cijfer") val cijfer: Double? = null,
    @SerialName("Vak") val vak: Vak? = null,
    @SerialName("Kolom") val kolom: GradeColumn? = null,
    @SerialName("DatumIngevoerd") val datumIngevoerd: String? = null,
    @SerialName("Inhoud") val inhoud: String? = null,
) {
    val vakNaam: String get() = vak?.omschrijving.orEmpty()
    val kolomNaam: String get() = kolom?.naam ?: kolom?.omschrijving.orEmpty()
    val cijferLabel: String get() = cijferStr ?: cijfer?.toString() ?: "?"
}

@Serializable
data class GradeResponse(
    @SerialName("Items") val items: List<Grade> = emptyList(),
)

object MagisterTime {
    private val fallback = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun dateTime(raw: String?): LocalDateTime? {
        if (raw.isNullOrBlank()) return null
        return try {
            LocalDateTime.parse(raw, fallback)
        } catch (_: Exception) {
            try {
                OffsetDateTime.parse(raw).toLocalDateTime()
            } catch (_: Exception) {
                null
            }
        }
    }
}
