package no.nav.aap.utbetal.hendelse.konsument

import no.nav.aap.utbetal.kodeverk.Status
import java.time.LocalDate

data class UtbetalingStatusHendelse(
    val status: Status,
    val detaljer: UtbetalingDetaljer? = null,
    val error: UtbetalingError? = null,
)

data class UtbetalingError(
    val statusKode: Int,
    val msg: String,
    val doc: String,
)

data class UtbetalingDetaljer(
    val ytelse: String,
    val linjer: List<UtbetalingLinje>,
)

data class UtbetalingLinje(
    val behandlingId: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val vedtakssats: UInt?,
    val beløp: UInt,
    val klassekode: String,
)
