package no.nav.aap.utbetal.server.prosessering

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.utbetal.klienter.helved.HelvedUtbetalingOppretter
import no.nav.aap.utbetal.klienter.helved.UtbetalingKlient
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.utbetal.utbetaling.UtbetalingJobbService
import no.nav.aap.utbetal.utbetaling.SakUtbetalingRepository
import no.nav.aap.utbetal.utbetaling.Utbetaling
import no.nav.aap.utbetal.utbetaling.UtbetalingBeregner
import no.nav.aap.utbetal.utbetaling.UtbetalingRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

class OverførTilØkonomiJobbUtfører(private val connection: DBConnection): JobbUtfører {

    private val log: Logger = LoggerFactory.getLogger(UtbetalingJobbService::class.java)

    override fun utfør(input: JobbInput) {
        val saksnummer = input.parameter("saksnummer")
        val behandlingsreferanse = input.parameter("behandlingsreferanse")
        val fom = LocalDate.parse(input.parameter("fom"))
        val tom = LocalDate.parse(input.parameter("tom"))
        val periode = Periode(fom ,tom)
        log.info("Overfører til økonomi for behandling: $behandlingsreferanse Periode: $periode")

        val (utbetaling, helvedUtbetaling) = opprettUtbetaling(Saksnummer(saksnummer), UUID.fromString(behandlingsreferanse), periode)
        UtbetalingKlient().iverksett(utbetaling.utbetalingRef, helvedUtbetaling)
        UtbetalingJobbService(connection).opprettSjekkKvitteringJobb(utbetaling.id!!)
    }

    private fun opprettUtbetaling(saksnummer: Saksnummer, behandlingsreferanse: UUID, periode: Periode): Pair<Utbetaling, no.nav.aap.utbetal.klienter.helved.Utbetaling> {
        val tilkjentYtelseRepo = TilkjentYtelseRepository(connection)
        val nyTilkjentYtelse = tilkjentYtelseRepo.hent(behandlingsreferanse) ?: throw IllegalArgumentException("Finner ikke tilkjent ytelse for behandling: $behandlingsreferanse")
        val forrigeTilkjentYtelse = nyTilkjentYtelse.forrigeBehandlingsreferanse?.let {tilkjentYtelseRepo.hent(it)}
        val sakUtbetaling = SakUtbetalingRepository(connection).hent(saksnummer) ?: throw IllegalArgumentException("Finner ikke sak")
        val utbetaling = UtbetalingBeregner().tilkjentYtelseTilUtbetaling(sakUtbetaling.id!!, forrigeTilkjentYtelse, nyTilkjentYtelse)
        val utbetalingId = UtbetalingRepository(connection).lagre(utbetaling)
        val helvedUtbetaling = HelvedUtbetalingOppretter().opprettUtbetaling(nyTilkjentYtelse, periode)
        return Pair(utbetaling.copy(id = utbetalingId), helvedUtbetaling)
    }

    companion object: Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return OverførTilØkonomiJobbUtfører(connection)
        }

        override fun type(): String {
            return "batch.overførTilØkonomi"
        }

        override fun navn(): String {
            return "Overfør tilkjent ytelse til økonomi"
        }

        override fun beskrivelse(): String {
            return "Overfør tilkjent ytelse til økonomi"
        }

        override fun retries(): Int {
            return 10 //TODO: hvor mange ganger skal vi prøve?
        }
    }
}