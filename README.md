# Mistral Magister

Onofficiële Android-client voor [Magister](https://www.magister.nl). Kotlin + Jetpack Compose,
Material 3 Expressive, dynamic color (Material You), animaties en haptics.

> Mistral Magister is **geen** product van Iddink/Magister. Werkt met elke Magister-school.
> Alleen de ingelogde leerling ziet de eigen data.

## Features

- **Login** via Magister OAuth2 + PKCE (Custom Tab), tokens AES-GCM versleuteld in de Android Keystore
- **Vandaag**: volgende les, laatste cijfer, rooster van vandaag, pull-to-refresh
- **Rooster**: 7 dagen vooruit, lesuitval/verplaatsing rood gemarkeerd
- **Cijfers**: laatste cijfers met kleur t.o.v. de 5,5-grens
- **Meldingen**: nieuwe cijfers en lesuitval/roosterwijzigingen via WorkManager (elke 30 min)
- **Live update**: doorlopende, stille notificatie met de huidige of volgende les
- **Haptics**: korte vibratiepatronen bij schoolkeuze, tabs, sync en uitloggen

## Inloggen (overgenomen uit de mags-flow)

1. Zoek je school (naam of `tenant.magister.net`-host).
2. *Inloggen* opent een Chrome Custom Tab op `accounts.magister.net/connect/authorize`.
3. Na het inloggen stuurt Magister je terug naar `m6loapp://oauth2redirect/`,
   wat de app opvangt via een intent-filter.
4. De app wisselt de code voor tokens (PKCE S256) en haalt het account op.

OAuth-configuratie (zelfde als de officiële app, Magister accepteert geen eigen public clients):

| Instelling | Waarde |
|---|---|
| client_id | `M6LOAPP` |
| redirect_uri | `m6loapp://oauth2redirect/` |
| response_type | `id_token code` |
| scope | `openid profile offline_access magister.mobile magister.ecs` |
| acr_values | `tenant:<host>` |
| code_challenge_method | `S256` |

API-calls gaan naar `https://<tenant>/api/...` met `Authorization: Bearer` + `X-API-Client-ID: M6LOAPP`.

## Bouwen

Vereist JDK 17+ en Android SDK 36.

```bash
export ANDROID_HOME=/pad/naar/android-sdk
./gradlew :app:testDebugUnitTest   # unit-tests (PKCE, OAuth, diff-logica)
./gradlew :app:assembleDebug
```

## Structuur

```text
app/src/main/java/nl/mistralmagister/
├── api/        endpoints, tenant-normalisatie, schoolzoeker
├── auth/       PKCE, OAuth-request-factory, versleutelde tokenstore
├── data/       DTO's, repository, container
├── haptics/    vibratiepatronen
├── notify/     kanalen, cijfer/lesuitval-meldingen, live update, diff-logica
├── work/       30-minuten achtergrondsync (WorkManager)
└── ui/         Compose-schermen (login, vandaag, rooster, cijfers, instellingen)
```
