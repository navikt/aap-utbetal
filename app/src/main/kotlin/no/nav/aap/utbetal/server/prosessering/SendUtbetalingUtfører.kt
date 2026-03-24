package no.nav.aap.utbetal.server.prosessering

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.utbetal.helved.tilUtbetalingMelding
import no.nav.aap.utbetal.hendelse.kafka.KafkaProdusentKonfig
import no.nav.aap.utbetal.hendelse.produsent.UtbetalingProdusent
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseRepository
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
            .oppdatereMeldeperiodeUtbetalingMapping(sakUtbetalingId, tilkjentYtelse)

        val utbetalingMelding = tilkjentYtelse.tilUtbetalingMelding(meldeperiodeUtbetalingMap)

        val utbetalingProdusent = UtbetalingProdusent(KafkaProdusentKonfig())
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