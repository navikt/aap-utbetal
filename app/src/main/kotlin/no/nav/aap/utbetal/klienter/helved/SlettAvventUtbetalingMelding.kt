package no.nav.aap.utbetal.klienter.helved

data class SlettAvventUtbetalingMelding(
    val sakId: String,
    val personident: String,
    val stønad: String = "AAP_UNDER_ARBEIDSAVKLARING",
    val saksbehandlerId: String ="Kelvin",
    val avvent: Avvent,
)