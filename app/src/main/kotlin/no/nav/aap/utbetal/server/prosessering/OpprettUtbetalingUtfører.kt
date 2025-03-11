package no.nav.aap.utbetal.server.prosessering

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.utbetal.utbetaling.SakUtbetalingRepository
import no.nav.aap.utbetal.utbetaling.Utbetaling
import no.nav.aap.utbetal.utbetaling.UtbetalingBeregner
import no.nav.aap.utbetal.utbetaling.UtbetalingJobbService
import no.nav.aap.utbetal.utbetaling.UtbetalingRepository
import no.nav.aap.utbetal.utbetaling.Utbetalinger
import no.nav.aap.utbetal.utbetaling.UtbetalingstidslinjeService
import no.nav.aap.utbetaling.UtbetalingStatus
import java.time.LocalDate
import java.util.UUID

class OpprettUtbetalingUtfører(private val connection: DBConnection): JobbUtfører {
    override fun utfør(input: JobbInput) {
        val saksnummer = input.parameter("saksnummer")
        val behandlingsreferanse = input.parameter("behandlingsreferanse")

        val utbetalinger = opprettUtbetalinger(
            saksnummer = Saksnummer(saksnummer),
            behandlingsreferanse = UUID.fromString(behandlingsreferanse)
        )

        val utbetalingJobbService = UtbetalingJobbService(connection)
        utbetalinger.alle()
            .filter {it.utbetalingStatus != UtbetalingStatus.INGEN_PERIODER }
            .forEach { utbetaling -> utbetalingJobbService.overførUtbetalingJobb(utbetaling.id!!) }
    }

    private fun opprettUtbetalinger(saksnummer: Saksnummer, behandlingsreferanse: UUID): Utbetalinger {
        val tilkjentYtelseRepo = TilkjentYtelseRepository(connection)
        val nyTilkjentYtelse = tilkjentYtelseRepo.hent(behandlingsreferanse) ?: throw IllegalArgumentException("Finner ikke tilkjent ytelse for behandling: $behandlingsreferanse")

        val sakUtbetaling = SakUtbetalingRepository(connection).hent(saksnummer) ?: throw IllegalArgumentException("Finner ikke sak")
        val utbetalingRepository = UtbetalingRepository(connection)
        val utbetalingstidslinjeService = UtbetalingstidslinjeService(tilkjentYtelseRepo, utbetalingRepository)
        val utbetalingTidslinje = utbetalingstidslinjeService.byggTidslinje(saksnummer)
        val utbetalinger = UtbetalingBeregner().tilkjentYtelseTilUtbetaling(sakUtbetaling.id!!, nyTilkjentYtelse, utbetalingTidslinje, LocalDate.now().minusDays(1)) //TODO: Blir i dag minus en dag riktig?

        return lagreUtbetalinger(utbetalinger)
    }

    private fun lagreUtbetalinger(utbetalinger: Utbetalinger): Utbetalinger {
        val utbetalingRepo = UtbetalingRepository(connection)
        val endringUtbetalinger = mutableListOf<Utbetaling>()
        utbetalinger.endringUtbetalinger.forEach { endringUtbetaling ->
            val utbetalingId = utbetalingRepo.lagre(endringUtbetaling)
            endringUtbetalinger.add(endringUtbetaling.copy(id = utbetalingId))
        }
        val utbetalingId = utbetalingRepo.lagre(utbetalinger.nyUtbetaling)
        val nyUtbetaling = utbetalinger.nyUtbetaling.copy(id = utbetalingId)

        return Utbetalinger(
            endringUtbetalinger = endringUtbetalinger,
            nyUtbetaling = nyUtbetaling

        )
    }


    companion object: Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return OpprettUtbetalingUtfører(connection)
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