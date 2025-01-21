package no.nav.aap.utbetal.server.prosessering

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.utbetal.utbetaling.UtbetalingJobbService
import no.nav.aap.utbetal.utbetaling.SakUtbetalingRepository
import no.nav.aap.utbetal.utbetaling.Utbetaling
import no.nav.aap.utbetal.utbetaling.UtbetalingBeregner
import no.nav.aap.utbetal.utbetaling.UtbetalingRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

class OverførTilØkonomiJobbUtfører(private val connection: DBConnection): JobbUtfører {

    private val log: Logger = LoggerFactory.getLogger(UtbetalingJobbService::class.java)

    override fun utfør(input: JobbInput) {
        val saksnummer = input.parameter("saksnummer")
        val behandlingsreferanse = input.parameter("behandlingsreferanse")
        log.info("Overfører til økonomi for behandling: $behandlingsreferanse")

        val utbetaling = opprettUtbetalingsplan(Saksnummer(saksnummer), UUID.fromString(behandlingsreferanse))
        // TODO: kall helved-utbetaling med utbetalingsplan
        UtbetalingJobbService(connection).opprettSjekkKvitteringJobb(utbetaling.id!!)
    }

    private fun opprettUtbetalingsplan(saksnummer: Saksnummer, behandlingsreferanse: UUID): Utbetaling {
        val tilkjentYtelseRepo = TilkjentYtelseRepository(connection)
        val nyTilkjentYtelse = tilkjentYtelseRepo.hent(behandlingsreferanse)
        if (nyTilkjentYtelse == null) {
            throw IllegalArgumentException("Finner ikke tilkjent ytelse for behandling: $behandlingsreferanse")
        }
        val sakUtbetaling = SakUtbetalingRepository(connection).hent(saksnummer) ?: throw IllegalArgumentException("Finner ikke sak")
        val forrigeTilkjentYtelse = nyTilkjentYtelse.forrigeBehandlingsreferanse?.let {tilkjentYtelseRepo.hent(it)}
        val utbetaling = UtbetalingBeregner().tilkjentYtelseTilUtbetaling(sakUtbetaling.id!!, forrigeTilkjentYtelse, nyTilkjentYtelse)
        val utbetalingId = UtbetalingRepository(connection).lagre(utbetaling)
        return utbetaling.copy(id = utbetalingId)
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