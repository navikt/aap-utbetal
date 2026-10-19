package no.nav.aap.utbetal.kodeverk

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
    /**
     * Egen status kun for denne modulen.
     * Ikke sendt enda, men ligger i kø for å bli sendt.
     */
    IKKE_SENDT,

}