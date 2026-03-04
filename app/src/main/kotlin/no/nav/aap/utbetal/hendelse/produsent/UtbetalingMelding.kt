package no.nav.aap.utbetal.hendelse.produsent

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Modell for melding som sendes til Kafka ved utbetaling.
 */
data class UtbetalingMelding(
    /** Saksnummer fra Kelvin. */
    val sakId: String,
    /** Behendlingsreferanse fra Kelvin. Kodens som Base64 for å unngå problemer med feltlengde på 30 tegn i Oppdrag. */
    val behandlingId:String,
    /** Fødselsnummer til mottaker av utbetalingen. */
    val ident: String,
    /** Alle utbetalingers om inngår i denne meldingen. */
    val utbetalinger: List<Utbetaling>,
    /** Tidspunkt for vedtaket som førte til utbetalingen. */
    val vedtakstidspunkt: LocalDateTime,
    /** Saksbehandlers ident. */
    val saksbehandling: String,
    /** Beslutters ident. */
    val beslutter: String,

    )

data class Utbetaling(
    /** Meldekortperiode i formatet YYYYMMDD-YYYYMMDD. */
    val meldeperiode: String,
    /** Dato for utbetalingen. */
    val dato: LocalDate,
    /** Sats i kroner per dag. */
    val sats: UInt,
    /** Utbetalt beløp i kroner per dag. */
    val utbetaltBeløp: UInt,
)