package no.nav.aap.utbetal.hendelse.konsument

import java.time.LocalDate


data class UtbetalingStatusHendelse(
    val status: Status,
    val detaljer: UtbetalingDetaljer,
    val error: UtbetalingError? = null,
)

enum class Status {
    /**
     * Status "OK" betyr at utbetalingen er mottatt og godkjent uten feil.
     */
    OK,
    /**
     * Status "FEILET" betyr at utbetalingen har en feil som må fikses før den går gjennom.
     */
    FEILET,
    /**
     * Status "MOTTATT" betyr at utbetalingen er mottatt men ikke videresendt til oppdrag.
     */
    MOTTATT,
    /**
     * Status "HOS_OPPDRAG" betyr at utbetalingen er sendt videre til oppdrag, men ok eller feilet status er enda ikke mottatt.
     */
    HOS_OPPDRAG,
    /**
     * Egen status kun for denne modulen.
     * Status "SENDT" betyr at melding er lagt på ut-topic, men har ikke fått noen status oppdatering enda.
     */
    SENDT,
}

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
