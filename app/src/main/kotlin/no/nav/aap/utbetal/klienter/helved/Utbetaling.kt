package no.nav.aap.utbetal.klienter.helved

import java.time.LocalDate
import java.time.LocalDateTime

data class Utbetaling(
    val sakId: String,
    val behandlingId: String,
    val personident: String,
    val vedtakstidspunkt: LocalDateTime,
    val stønad: String = "AAP",
    val beslutterId: String,
    val saksbehandlerId: String,
    val periodeType: String = "UKEDAG",
    val perioder: List<Utbetalingsperiode>,

)
data class Utbetalingsperiode(
    val fom: LocalDate,
    val tom: LocalDate,
    val beløp: Int,
    val betalendeEnhet: String? = null,
    val fastsattDagsats: Int,
)