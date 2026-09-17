package no.nav.aap.utbetal.server.prosessering.nytt_grensesnitt

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.JobbInput
import no.nav.aap.utbetal.hendelse.kafka.KafkaProdusentKonfig
import no.nav.aap.utbetal.hendelse.konsument.Status
import no.nav.aap.utbetal.hendelse.konsument.UtbetalingDetaljer
import no.nav.aap.utbetal.hendelse.konsument.UtbetalingStatusHendelse
import no.nav.aap.utbetal.hendelse.produsent.UtbetalingProdusent
import no.nav.aap.utbetal.hendelse.produsent.UtbetalingsmeldingSender
import no.nav.aap.utbetal.tilkjentytelse.UtbetalingStatusRepository
import java.util.UUID

class SendUtbetalingsmeldingUtfører(
    private val connection: DBConnection,
    private val utbetalingsmeldingSenderFactory: () -> UtbetalingsmeldingSender = senderFactory,
    ): JobbUtfører {

    override fun utfør(input: JobbInput) {
        val behandlingsreferanse = UUID.fromString(input.parameter("behandlingsreferanse"))
        val tilkjentYtelseId = input.parameter("tilkjentYtelseId").toLong()
        val utbetalingsmelding = input.parameter("utbetalingsmelding")

        // Status settes til sendt. Ruller tilbake dersom sending feiler.
        settStatusTilSendt(tilkjentYtelseId, behandlingsreferanse)

        utbetalingsmeldingSenderFactory().produser(behandlingsreferanse.toString(), utbetalingsmelding)
    }

    private fun settStatusTilSendt(tilkjentYtelseId: Long, referanse: UUID) {
        UtbetalingStatusRepository(connection).oppdaterUtbetalingsstatus(
            tilkjentYtelseId = tilkjentYtelseId,
            referanse = referanse,
            UtbetalingStatusHendelse(
                status = Status.SENDT,
                detaljer = UtbetalingDetaljer(
                    ytelse = "AAP",
                    // Lagrer tom liste ved status SENDT, siden vi ikke har fått noen respons fra utsjekk enda. Linjene vil
                    // bli oppdatert når vi får respons fra utsjekk i form av en utbetaling-status-hendelse (som blir
                    // håndtert av UtbetalingStatusKonsument)
                    linjer = listOf(),
                )
            )
        )
    }

    companion object: Jobb {
        // Overstyrbar kun for tester, slik at Kafka-sending kan mockes uten en ekte broker.
        // Skal ikke overstyres i produksjonskode.
        var senderFactory: () -> UtbetalingsmeldingSender = { UtbetalingProdusent(KafkaProdusentKonfig()) }

        override fun konstruer(connection: DBConnection): JobbUtfører {
            return SendUtbetalingsmeldingUtfører(connection)
        }

        override fun type(): String {
            return "batch.sendUtbetalingsmelding"
        }

        override fun navn(): String {
            return "Send utbetalingsmelding"
        }

        override fun beskrivelse(): String {
            return "Sender utbetalingsmelding til utsjekk"
        }
    }

}