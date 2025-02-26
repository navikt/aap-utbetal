package no.nav.aap.utbetal.server.prosessering

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.utbetal.utbetaling.SakUtbetalingRepository
import no.nav.aap.utbetal.utbetaling.UtbetalingBeregner
import no.nav.aap.utbetal.utbetaling.UtbetalingJobbService
import no.nav.aap.utbetal.utbetaling.UtbetalingRepository import java.time.LocalDate
import java.util.UUID

class OpprettUtbetalingUtfører(private val connection: DBConnection): JobbUtfører {
    override fun utfør(input: JobbInput) {
        val saksnummer = input.parameter("saksnummer")
        val behandlingsreferanse = input.parameter("behandlingsreferanse")
        val fom = LocalDate.parse(input.parameter("fom"))
        val tom = LocalDate.parse(input.parameter("tom"))
        val periode = Periode(fom ,tom)

        val utbetalingId = opprettUtbetaling(
            saksnummer = Saksnummer(saksnummer),
            behandlingsreferanse = UUID.fromString(behandlingsreferanse),
            periode = periode
        )

        UtbetalingJobbService(connection).overførUtbetalingJobb(utbetalingId)
    }

    private fun opprettUtbetaling(saksnummer: Saksnummer, behandlingsreferanse: UUID, periode: Periode): Long {
        val tilkjentYtelseRepo = TilkjentYtelseRepository(connection)
        val nyTilkjentYtelse = tilkjentYtelseRepo.hent(behandlingsreferanse) ?: throw IllegalArgumentException("Finner ikke tilkjent ytelse for behandling: $behandlingsreferanse")
        val forrigeTilkjentYtelse = nyTilkjentYtelse.forrigeBehandlingsreferanse?.let {tilkjentYtelseRepo.hent(it)}


        val sakUtbetaling = SakUtbetalingRepository(connection).hent(saksnummer) ?: throw IllegalArgumentException("Finner ikke sak")
        val utbetaling = UtbetalingBeregner().tilkjentYtelseTilUtbetaling(sakUtbetaling.id!!, nyTilkjentYtelse, forrigeTilkjentYtelse, periode)

        return UtbetalingRepository(connection).lagre(utbetaling)
    }


    companion object: Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return OverførTilØkonomiJobbUtfører(connection)
        }

        override fun type(): String {
            return "batch.opprettUtbetaling"
        }

        override fun navn(): String {
            return "Opprett utbetaling"
        }

        override fun beskrivelse(): String {
            return "Opprett utbetaling"
        }

    }

}