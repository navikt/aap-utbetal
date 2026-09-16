package no.nav.aap.utbetal.test

import no.nav.aap.utbetal.hendelse.produsent.UtbetalingsmeldingSender
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Fake-implementasjon av [UtbetalingsmeldingSender] som brukes i tester for å unngå avhengighet til en ekte
 * Kafka-broker. Fanger opp meldinger som ellers ville blitt sendt til topic `aap.utbetaling.v1` via
 * [no.nav.aap.utbetal.server.prosessering.nytt_grensesnitt.SendUtbetalingsmeldingUtfører].
 *
 * Bruk: overstyr
 * `no.nav.aap.utbetal.server.prosessering.nytt_grensesnitt.SendUtbetalingsmeldingUtfører.Companion.senderFactory`
 * med `{ fakeUtbetalingsmeldingSender }` før testene kjører, og tøm mellom hver test med [clear].
 */
class FakeUtbetalingsmeldingSender : UtbetalingsmeldingSender {

    data class SendtMelding(val nøkkel: String, val melding: String)

    /** Alle meldinger som er "sendt", i rekkefølge. Nøkkel er alltid behandlingsreferansen (som streng). */
    val sendteMeldinger: MutableList<SendtMelding> = CopyOnWriteArrayList()

    override fun produser(key: String, value: String) {
        sendteMeldinger.add(SendtMelding(key, value))
    }

    fun sisteMeldingFor(nøkkel: String): String? = sendteMeldinger.lastOrNull { it.nøkkel == nøkkel }?.melding

    fun clear() = sendteMeldinger.clear()
}
