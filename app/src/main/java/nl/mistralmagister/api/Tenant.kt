package nl.mistralmagister.api

/** Normaliseert een Magister-tenant hostname, bijv. "citadel" -> "citadel.magister.net". */
object Tenant {
    fun normalize(raw: String): String {
        var value = raw.trim().lowercase()
        value = value.removePrefix("https://").removePrefix("http://")
        value = value.substringBefore('/').substringBefore('?')
        if (value.isBlank()) return ""
        if (!value.contains('.')) value = "$value.magister.net"
        if (value == "citadelcollege.magister.net") return "citadel.magister.net"
        return value
    }

    fun looksLikeHost(raw: String): Boolean {
        val trimmed = raw.trim()
        if (trimmed.isBlank() || ' ' in trimmed) return false
        if (!trimmed.contains('.') && !trimmed.startsWith("http", ignoreCase = true)) return false
        val host = normalize(trimmed)
        return host.endsWith(".magister.net") && host != ".magister.net"
    }
}

data class School(
    val name: String,
    val host: String,
    val city: String = "",
)

object KnownSchools {
    val suggestions: List<School> = listOf(
        School("Citadel College", "citadel.magister.net", "Nijmegen / Lent"),
        School("Canisius College", "canisius.magister.net", "Nijmegen"),
        School("Kandinsky College", "kandinsky.magister.net", "Nijmegen"),
        School("NSG Groenewoud", "nsggroenewoud.magister.net", "Nijmegen"),
        School("Stedelijk Gymnasium Nijmegen", "sgnijmegen.magister.net", "Nijmegen"),
        School("SSGN", "ssgn.magister.net", "Nijmegen"),
        School("Mondial College", "mondial.magister.net", "Nijmegen"),
        School("Pax Christi College", "paxchristicollege.magister.net", "Druten"),
        School("Maaswaal College", "maaswaalcollege.magister.net", "Wijchen"),
    )

    fun search(query: String): List<School> {
        val q = query.trim().lowercase()
        if (q.length < 2) return emptyList()
        val hits = suggestions.filter {
            it.name.lowercase().contains(q) || it.host.contains(q) || it.city.lowercase().contains(q)
        }
        return hits.ifEmpty {
            if (Tenant.looksLikeHost(query)) listOf(School(query.trim(), Tenant.normalize(query)))
            else emptyList()
        }
    }
}
