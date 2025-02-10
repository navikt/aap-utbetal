package no.nav.aap.utbetal.klienter.helved

import java.time.LocalDate
import java.time.LocalDateTime

data class Utbetaling(
    val sakId: String,
    val behandlingId: String,
    val personident: String,
    val vedtakstidspunkt: LocalDateTime,
    val stønad: String = "AAP_UNDER_ARBEIDSAVKLARING",
    val beslutterId: String,
    val saksbehandlerId: String,
    val periodeType: String = "UKEDAG",
    val perioder: List<Utbetalingsperiode>,

)
data class Utbetalingsperiode(
    val fom: LocalDate,
    val tom: LocalDate,
    val beløp: UInt,
    val betalendeEnhet: String? = null,
    val fastsattDagsats: UInt? = null,
)