package no.nav.aap.utbetal.helved

import java.time.LocalDateTime

/**
 * Modell for melding som sendes til Kafka ved utbetaling.
 */
data class UtbetalingMelding(
    /** Angir om det skal gjøres en simulering. */
    val dryrun: Boolean = false,
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
    /** Avvent utbetaling. */
    val avvent: Avvent? = null,
)

data class Avvent(
    /** Fra og med. */
    val fom: String,
    /*** Til og med. */
    val tom: String,
    /** Når beløpet skal overføres etter en avventet utbetaling. */
    val overføres: String?,
    /** Årsak til avvent utbetaling. */
    val årsak: String? = null,
    /** Om det er en feilregistrering. Brukes ikke per nå. */
    val feilregistrering: Boolean = false,
)

data class Utbetaling(
    /** Unik id for utbetalingsperiode. */
    val id: String,
    /** Fra og med. */
    val fom: String,
    /*** Til og med. */
    val tom: String,
    /** Sats i kroner per dag. */
    val sats: UInt,
    /** Utbetalt beløp i kroner per dag. */
    val utbetaltBeløp: UInt,
)