package no.nav.aap.utbetal.utbetaling

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.miljo.Miljø
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
        val utbetalingTidslinje = lagUtbetalingTidslinje(nyTilkjentYtelse.saksnummer)

        val utbetalingMedSlettingAvAvventPeriode = opprettUtbetalingMedSlettingAvAvventPeriode(nyTilkjentYtelse)
        val utbetalinger = UtbetalingBeregner().tilkjentYtelseTilUtbetaling(nyTilkjentYtelse, utbetalingTidslinje)

        return utbetalinger.copy(utbetalingMedSlettingAvAvventPeriode = utbetalingMedSlettingAvAvventPeriode)
    }

    private fun opprettUtbetalingMedSlettingAvAvventPeriode(nyTilkjentYtelse: TilkjentYtelse): Utbetaling? {
        if (!Miljø.erProd()) {
            val avventHistorikk =
                TilkjentYtelseRepository(connection).hentTilkjentYtelseAvventHistorikk(nyTilkjentYtelse.saksnummer)
                    .filter { it.behandlingRef != nyTilkjentYtelse.behandlingsreferanse }
            if (avventHistorikk.isNotEmpty()) {
                val forrigeAvventPeriode = avventHistorikk.last()
                if (!forrigeAvventPeriode.feilregistrering) {
                    return UtbetalingMedSlettingAvAvventUtbetalingBeregninger().opprettUtbetalingMedSlettingAvAvventUtbetaling(
                        utbetalingRef = UUID.randomUUID(), //TODO skal denne være random, eller skal vi bruke en fra tidligere utbetalinger? Bruker en ny random inntil videre for å unngå kollisjon med tidligere.
                        forrigeAvventPeriode = forrigeAvventPeriode,
                        nyTilkjentYtelse = nyTilkjentYtelse
                    )
                }
            }
        }
        return null
    }


    fun lagUtbetalingTidslinje(saksnummer: Saksnummer): Tidslinje<UtbetalingData> {
        val utbetalinger = UtbetalingRepository(connection).hent(saksnummer)
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

        val utbetalingMedSlettingAvAvventPeriode = if (utbetalinger.utbetalingMedSlettingAvAvventPeriode != null) {
            val utbetalingId = utbetalingRepo.lagre(sakUtbetaling.id!!, utbetalinger.utbetalingMedSlettingAvAvventPeriode)
            utbetalinger.utbetalingMedSlettingAvAvventPeriode.copy(id = utbetalingId)
        } else {
            null
        }

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
            utbetalingMedSlettingAvAvventPeriode = utbetalingMedSlettingAvAvventPeriode,
            endringUtbetalinger = endringUtbetalinger,
            nyeUtbetalinger = nyeUtbetalinger
        )
    }

}