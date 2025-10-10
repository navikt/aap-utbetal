package no.nav.aap.utbetal.utbetaling

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelse
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseRepository
import java.util.UUID

class UtbetalingService(private val connection: DBConnection) {

    fun simulerOpprettelseAvUtbetalinger(nyTilkjentYtelse: TilkjentYtelse): Utbetalinger {
        return opprettUtbetalinger(nyTilkjentYtelse)
    }

    fun opprettUtbetalinger(behandlingsreferanse: UUID): Utbetalinger {
        val nyTilkjentYtelse = TilkjentYtelseRepository(connection).hent(behandlingsreferanse) ?: throw IllegalArgumentException("Finner ikke tilkjent ytelse for behandling: $behandlingsreferanse")
        val utbetalinger = opprettUtbetalinger(nyTilkjentYtelse)
        return lagreUtbetalinger(nyTilkjentYtelse.saksnummer, utbetalinger)
    }

    private fun opprettUtbetalinger(nyTilkjentYtelse: TilkjentYtelse): Utbetalinger {
        val utbetalingListe = UtbetalingRepository(connection).hent(nyTilkjentYtelse.saksnummer)

        val utbetalingTidslinje = byggTidslinje(utbetalingListe)
        val utbetalinger = UtbetalingBeregner().tilkjentYtelseTilUtbetaling(nyTilkjentYtelse, utbetalingTidslinje)

        return utbetalinger
    }

    private fun byggTidslinje(utbetalinger: List<Utbetaling>): Tidslinje<UtbetalingData> {
        val segmenter = utbetalinger.flatMap { utbetaling ->
            utbetaling.perioder.map { periode ->
                Segment(periode = periode.periode, UtbetalingData(
                    utbetalingRef = utbetaling.utbetalingRef,
                    beløp = periode.beløp,
                    fastsattDagsats = periode.fastsattDagsats,
                    utbetalingsdato = periode.utbetalingsdato
                ))

            }
        }
        return Tidslinje(segmenter)
    }

    private fun lagreUtbetalinger(saksnummer: Saksnummer, utbetalinger: Utbetalinger): Utbetalinger {
        val sakUtbetaling = SakUtbetalingRepository(connection).hent(saksnummer) ?: throw IllegalArgumentException("Finner ikke sak")

        val utbetalingRepo = UtbetalingRepository(connection)

        val endringUtbetalinger = mutableListOf<Utbetaling>()
        utbetalinger.endringUtbetalinger.forEach { endringUtbetaling ->
            val utbetalingId = utbetalingRepo.lagre(sakUtbetaling.id!!, endringUtbetaling)
            endringUtbetalinger.add(endringUtbetaling.copy(id = utbetalingId))
        }

        val nyeUtbetalinger = mutableListOf<Utbetaling>()
        utbetalinger.nyeUtbetalinger.forEach { nyUtbetaling ->
            val utbetalingId = utbetalingRepo.lagre(sakUtbetaling.id!!, nyUtbetaling)
            nyeUtbetalinger.add(nyUtbetaling.copy(id = utbetalingId))
        }

        return Utbetalinger(
            endringUtbetalinger = endringUtbetalinger,
            nyeUtbetalinger = nyeUtbetalinger
        )
    }

}