package no.nav.aap.utbetal.server.prosessering

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.utbetal.helved.tilUtbetalingMelding
import no.nav.aap.utbetal.hendelse.kafka.KafkaProdusentKonfig
import no.nav.aap.utbetal.hendelse.konsument.Status
import no.nav.aap.utbetal.hendelse.konsument.UtbetalingDetaljer
import no.nav.aap.utbetal.hendelse.konsument.UtbetalingStatusHendelse
import no.nav.aap.utbetal.hendelse.produsent.UtbetalingProdusent
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.utbetal.tilkjentytelse.UtbetalingStatusRepository
import no.nav.aap.utbetal.utbetaling.MeldeperiodeUtbetalingMappingRepository
import java.util.*

class SendUtbetalingUtfører(private val connection: DBConnection): JobbUtfører {
    override fun utfør(input: JobbInput) {
        //OBS: sakId er i dette tilfellet sak_utbetaling_id siden vi ikke har sak_id i utbetalings-appen.
        val sakUtbetalingId = input.sakId()
        val behandlingsreferanse = UUID.fromString(input.parameter("behandlingsreferanse"))

        val tilkjentYtelse = TilkjentYtelseRepository(connection).hent(behandlingsreferanse)
            ?: throw IllegalArgumentException("Finner ikke tilkjent ytelse for behandling: $behandlingsreferanse")

        val meldeperiodeUtbetalingMap = MeldeperiodeUtbetalingMappingRepository(connection)
            .oppdatereMeldeperiodeUtbetalingMapping(sakUtbetalingId, tilkjentYtelse, true)

        val utbetalingMelding = tilkjentYtelse.tilUtbetalingMelding(meldeperiodeUtbetalingMap)

        val utbetalingProdusent = UtbetalingProdusent(KafkaProdusentKonfig())


        // Lagrer utbetaling status SENDT før vi sender utbetalingshendelsen, slik at vi har en status i databasen som
        // indikerer at vi har sendt utbetalingen til utsjekk. Hvis vi skulle fått en feil i det å sende ut meldingen
        // til utsjekk, så vil vi fortsatt ha en status i databasen som indikerer at vi har forsøkt å sende utbetalingen.
        UtbetalingStatusRepository(connection).lagre(tilkjentYtelse, UtbetalingStatusHendelse(
            status = Status.SENDT,
            detaljer = UtbetalingDetaljer(
                ytelse = "AAP",
                // Lagrer tom liste ved status SENDT, siden vi ikke har fått noen respons fra utsjekk enda. Linjene vil
                // bli oppdatert når vi får respons fra utsjekk i form av en utbetaling-status-hendelse (som blir
                // håndtert av UtbetalingStatusKonsument)
                linjer = listOf(),
            )
        ))

        utbetalingProdusent.sendUtbetalingHendelse(behandlingsreferanse.toString(), utbetalingMelding)
    }

    companion object: Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return SendUtbetalingUtfører(connection)
        }

        override fun type(): String {
            return "batch.sendUtbetaling"
        }

        override fun navn(): String {
            return "Sender utbetaling"
        }

        override fun beskrivelse(): String {
            return "Sender utbetaling på Kafka grensesnitt til Utsjekk"
        }
    }

}