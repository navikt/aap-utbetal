package no.nav.aap.utbetal.hendelse.produsent

/**
 * Minimalt grensesnitt for å sende en utbetalingsmelding (nøkkel/verdi) videre, typisk til Kafka.
 *
 * Finnes som eget grensesnitt slik at [no.nav.aap.utbetal.server.prosessering.nytt_grensesnitt.SendUtbetalingsmeldingUtfører]
 * kan testes uten en ekte Kafka-broker: tester kan overstyre
 * [no.nav.aap.utbetal.server.prosessering.nytt_grensesnitt.SendUtbetalingsmeldingUtfører.Companion.senderFactory]
 * med en fake-implementasjon.
 */
interface UtbetalingsmeldingSender {
    fun produser(key: String, value: String)
}
