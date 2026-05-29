package no.nav.aap.utbetal.migrering

data class MigreringDto(
    val maxAntall: Int,
    val dryRun: Boolean = true,
)

data class MigrerSakDto(
    val saksnummer: String,
    val dryRun: Boolean = true,
)

data class MigreringsresultatDto(
    val migrerteSaker: List<String>,
    val feiledeMigreringer: List<String>
)
