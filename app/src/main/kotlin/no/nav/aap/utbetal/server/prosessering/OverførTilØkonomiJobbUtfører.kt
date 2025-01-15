package no.nav.aap.utbetal.server.prosessering

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.utbetal.utbetaling.UtbetalingJobbService
import no.nav.aap.utbetal.utbetalingsplan.Utbetalingsplan
import no.nav.aap.utbetal.utbetalingsplan.UtbetalingsplanBeregner
import no.nav.aap.utbetal.utbetalingsplan.UtbetalingsplanRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

class OverførTilØkonomiJobbUtfører(private val connection: DBConnection): JobbUtfører {

    private val log: Logger = LoggerFactory.getLogger(UtbetalingJobbService::class.java)

    override fun utfør(input: JobbInput) {
        val behandlingsreferanse = input.parameter("behandlingsreferanse")
        log.info("Overfører til økonomi for behandling: $behandlingsreferanse")

        opprettUtbetalingsplan(UUID.fromString(behandlingsreferanse))
        // TODO: kall helved-utbetaling med utbetalingsplan
        // TODO: oppdater status på utbetalingsstaus til SENDT
        UtbetalingJobbService(connection).opprettSjekkKvitteringJobb(UUID.fromString(behandlingsreferanse))
    }

    private fun opprettUtbetalingsplan(behandlingsreferanse: UUID): Utbetalingsplan {
        val tilkjentYtelseRepo = TilkjentYtelseRepository(connection)
        val nyTilkjentYtelse = tilkjentYtelseRepo.hent(behandlingsreferanse)
        if (nyTilkjentYtelse == null) {
            throw IllegalArgumentException("Finner ikke tilkjent ytelse for behanndling: $behandlingsreferanse")
        }
        val forrigeTilkjentYtelse = nyTilkjentYtelse.forrigeBehandlingsreferanse?.let {tilkjentYtelseRepo.hent(it)}
        val utbetalingsplan = UtbetalingsplanBeregner().tilkjentYtelseTilUtbetalingsplan(forrigeTilkjentYtelse, nyTilkjentYtelse)
        UtbetalingsplanRepository(connection).lagre(utbetalingsplan)
        return utbetalingsplan
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