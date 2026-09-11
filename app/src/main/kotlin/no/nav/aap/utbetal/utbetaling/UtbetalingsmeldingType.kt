package no.nav.aap.utbetal.utbetaling

enum class UtbetalingsmeldingType {
    /** Vanlig utbetalingsmelding */
    UTBETALING,
    /** Slett avvent utbetaling periode melding */
    SLETT_AVVENT_PERIODE,
    /** Migrert utbetalingsmelding fra gammelt grensesnitt. */
    MIGRERT_UTBETALING,
    /** Migrert slett avvent utbetalingsperiode melding fra gammelt grensesnitt. */
    MIGRERT_SLETT_AVVENT_PERIODE,

}