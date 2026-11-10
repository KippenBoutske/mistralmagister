package nl.mistralmagister.api

/**
 * Alle Magister-URL's op een plek. Login-flow gebaseerd op de werkende
 * M6LOAPP-configuratie zoals ook gebruikt door de officiële app en
 * open-source clients (Otarium, Discipulus, Mags).
 */
object MagisterEndpoints {
    const val ACCOUNTS_HOST = "https://accounts.magister.net"
    const val AUTHORIZE = "$ACCOUNTS_HOST/connect/authorize"
    const val TOKEN = "$ACCOUNTS_HOST/connect/token"

    /** Client-id van de officiële app; Magister accepteert geen eigen public clients. */
    const val CLIENT_ID = "M6LOAPP"
    const val REDIRECT_URI = "m6loapp://oauth2redirect/"
    const val SCOPE = "openid profile offline_access magister.mobile magister.ecs"
    const val RESPONSE_TYPE = "id_token code"
    const val CODE_CHALLENGE_METHOD = "S256"

    fun tenantRoot(host: String): String = "https://$host"

    fun account(root: String) = "$root/api/account"

    fun appointments(root: String, personId: Int, from: String, to: String, status: Int = 0) =
        "$root/api/personen/$personId/afspraken?status=$status&van=$from&tot=$to"

    fun recentGrades(root: String, personId: Int, top: Int = 50, skip: Int = 0) =
        "$root/api/personen/$personId/cijfers/laatste?top=$top&skip=$skip"
}
