package no.nav.aap.utbetal.server.prosessering

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.utbetal.hendelse.kafka.KafkaProdusentKonfig
import no.nav.aap.utbetal.hendelse.produsent.Utbetaling
import no.nav.aap.utbetal.hendelse.produsent.UtbetalingMelding
import no.nav.aap.utbetal.hendelse.produsent.UtbetalingProdusent
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelse
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.utbetaling.helved.toBase64
import java.util.UUID

class SendUtbetalingUtfører(private val connection: DBConnection): JobbUtfører {
    override fun utfør(input: JobbInput) {

        val behandlingsreferanse = UUID.fromString(input.parameter("behandlingsreferanse"))
        val tilkjentYtelse = TilkjentYtelseRepository(connection).hent(behandlingsreferanse)
            ?: throw IllegalArgumentException("Finner ikke tilkjent ytelse for behandling: $behandlingsreferanse")

        val utbetalingMelding = tilkjentYtelse.tilUtbetalingMelding()

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

private fun TilkjentYtelse.tilUtbetalingMelding(): UtbetalingMelding {
    val utbetalingMelding = UtbetalingMelding(
        sakId = this.saksnummer.toString(),
        behandlingId = this.behandlingsreferanse.toBase64(),
        ident = this.personIdent,
        utbetalinger = this.perioder.tilUtbetalinger(),
        vedtakstidspunkt = this.vedtakstidspunkt,
        saksbehandling = this.saksbehandlerId,
        beslutter = this.beslutterId,
    )
    return utbetalingMelding
}

private val kompaktFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")

private fun List<TilkjentYtelsePeriode>.tilUtbetalinger() =
    this.map { tyPeriode ->
        val meldeperiode = tyPeriode.detaljer.meldeperiode
            ?: error("Meldeperiode må være satt for å kunne sende utbetaling.")
        Utbetaling(
            meldeperiode = "${meldeperiode.fom.format(kompaktFormatter)}-${meldeperiode.tom.format(kompaktFormatter)}",
            periode = tyPeriode.periode,
            sats = tyPeriode.detaljer.dagsats.avrundet(),
            utbetaltBeløp = tyPeriode.detaljer.redusertDagsats.avrundet(),
        )
    }

//Siden Beløp her altid er heltall, så holder det å trunkere til UInt.
private fun Beløp.avrundet() = this.verdi.toInt().toUInt()