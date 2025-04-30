package no.nav.aap.utbetal.utbetaling

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseRepository
import java.util.UUID

class UtbetalingService(private val connection: DBConnection) {

    fun opprettUtbetalinger(saksnummer: Saksnummer, behandlingsreferanse: UUID, lagre: Boolean = true): Utbetalinger {
        val tilkjentYtelseRepo = TilkjentYtelseRepository(connection)
        val nyTilkjentYtelse = tilkjentYtelseRepo.hent(behandlingsreferanse) ?: throw IllegalArgumentException("Finner ikke tilkjent ytelse for behandling: $behandlingsreferanse")
        val sakUtbetaling = SakUtbetalingRepository(connection).hent(saksnummer) ?: throw IllegalArgumentException("Finner ikke sak")
        val utbetalingRepository = UtbetalingRepository(connection)
        val utbetalingListe = utbetalingRepository.hent(saksnummer)
        val utbetalingTidslinje = byggTidslinje(utbetalingListe)
        val utbetalinger = UtbetalingBeregner().tilkjentYtelseTilUtbetaling(sakUtbetaling.id!!, nyTilkjentYtelse, utbetalingTidslinje)
        if (lagre) {
            return lagreUtbetalinger(utbetalinger)
        }
        return utbetalinger
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


}