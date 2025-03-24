package no.nav.aap.utbetal.server.prosessering

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.utbetal.utbetaling.SakUtbetalingRepository
import no.nav.aap.utbetal.utbetaling.Utbetaling
import no.nav.aap.utbetal.utbetaling.UtbetalingBeregner
import no.nav.aap.utbetal.utbetaling.UtbetalingData
import no.nav.aap.utbetal.utbetaling.UtbetalingJobbService
import no.nav.aap.utbetal.utbetaling.UtbetalingRepository
import no.nav.aap.utbetal.utbetaling.Utbetalinger
import java.util.*

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
            .forEach { utbetaling -> utbetalingJobbService.overførUtbetalingJobb(utbetaling.id!!) }
    }

    private fun opprettUtbetalinger(saksnummer: Saksnummer, behandlingsreferanse: UUID): Utbetalinger {
        val tilkjentYtelseRepo = TilkjentYtelseRepository(connection)
        val nyTilkjentYtelse = tilkjentYtelseRepo.hent(behandlingsreferanse) ?: throw IllegalArgumentException("Finner ikke tilkjent ytelse for behandling: $behandlingsreferanse")
        val sakUtbetaling = SakUtbetalingRepository(connection).hent(saksnummer) ?: throw IllegalArgumentException("Finner ikke sak")
        val utbetalingRepository = UtbetalingRepository(connection)
        val utbetalingListe = utbetalingRepository.hent(saksnummer)
        val utbetalingTidslinje = byggTidslinje(utbetalingListe)
        val utbetalinger = UtbetalingBeregner().tilkjentYtelseTilUtbetaling(sakUtbetaling.id!!, nyTilkjentYtelse, utbetalingTidslinje)
        return lagreUtbetalinger(utbetalinger)
    }

    private fun byggTidslinje(utbetalinger: List<Utbetaling>): Tidslinje<UtbetalingData> {
        val segmenter = utbetalinger.flatMap { utbetaling ->
            utbetaling.perioder.map { periode ->
                Segment<UtbetalingData>(periode = periode.periode, UtbetalingData(
                    utbetalingRef = utbetaling.utbetalingRef,
                    beløp = periode.beløp,
                    fastsattDagsats = periode.fastsattDagsats,
                    utbetalingsdato = periode.utbetalingsdato
                ))

            }
        }
        return Tidslinje<UtbetalingData>(segmenter)
    }

    private fun lagreUtbetalinger(utbetalinger: Utbetalinger): Utbetalinger {
        val utbetalingRepo = UtbetalingRepository(connection)

        val endringUtbetalinger = mutableListOf<Utbetaling>()
        utbetalinger.endringUtbetalinger.forEach { endringUtbetaling ->
            val utbetalingId = utbetalingRepo.lagre(endringUtbetaling)
            endringUtbetalinger.add(endringUtbetaling.copy(id = utbetalingId))
        }

        val nyeUtbetalinger = mutableListOf<Utbetaling>()
        utbetalinger.nyeUtbetalinger.forEach { nyUtbetaling ->
            val utbetalingId = utbetalingRepo.lagre(nyUtbetaling)
            nyeUtbetalinger.add(nyUtbetaling.copy(id = utbetalingId))
        }

        return Utbetalinger(
            endringUtbetalinger = endringUtbetalinger,
            nyeUtbetalinger = nyeUtbetalinger
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