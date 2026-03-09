package no.nav.aap.utbetal.hendelse.produsent

import no.nav.aap.komponenter.type.Periode
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
    /** Meldekortperiode identifikator. */
    val meldeperiode: String,
    /** Periode for utbetalingen. */
    val periode: Periode,
    /** Sats i kroner per dag. */
    val sats: UInt,
    /** Utbetalt beløp i kroner per dag. */
    val utbetaltBeløp: UInt,
)