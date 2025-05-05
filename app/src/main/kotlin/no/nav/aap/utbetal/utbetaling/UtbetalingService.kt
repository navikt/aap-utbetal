package no.nav.aap.utbetal.utbetaling

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelse
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseRepository
import java.util.UUID

class UtbetalingService(private val connection: DBConnection) {

    fun simulerOpprettelseAvUtbetalinger(nyTilkjentYtelse: TilkjentYtelse): Utbetalinger {
        return opprettUtbetalinger(nyTilkjentYtelse, false)
    }

    fun opprettUtbetalinger(behandlingsreferanse: UUID): Utbetalinger {
        val nyTilkjentYtelse = TilkjentYtelseRepository(connection).hent(behandlingsreferanse) ?: throw IllegalArgumentException("Finner ikke tilkjent ytelse for behandling: $behandlingsreferanse")
        return opprettUtbetalinger(nyTilkjentYtelse, true)
    }

    private fun opprettUtbetalinger(nyTilkjentYtelse: TilkjentYtelse, lagre: Boolean = true): Utbetalinger {
        val sakUtbetaling = SakUtbetalingRepository(connection).hent(nyTilkjentYtelse.saksnummer) ?: throw IllegalArgumentException("Finner ikke sak")
        val utbetalingListe = UtbetalingRepository(connection).hent(nyTilkjentYtelse.saksnummer)

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