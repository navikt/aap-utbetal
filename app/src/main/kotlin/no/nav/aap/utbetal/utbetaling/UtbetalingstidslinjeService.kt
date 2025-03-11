package no.nav.aap.utbetal.utbetaling

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseRepository
import java.time.LocalDate
import java.util.UUID

data class UtbetalingData(
    val utbetalingRef: UUID,
    val beløp: UInt,
    val fastsattDagsats: UInt,
    val utbetalingsdato: LocalDate
)

class UtbetalingstidslinjeService(
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val utbetalingRepository: UtbetalingRepository,
) {

    fun byggTidslinje(saksnummer: Saksnummer): Tidslinje<UtbetalingData> {
        val rekkefølgeTilkjentYtelse = tilkjentYtelseRepository.finnRekkefølgeTilkjentYtelse(saksnummer)
        var utbetalingTidslinje = Tidslinje<UtbetalingData>()
        //TODO: Kan optimaliseres ved å lage et query som henter ut alle utbetalinger i en spørring.
        rekkefølgeTilkjentYtelse.map {it.behandlingRef}.forEach { behandlingRef ->
            val segmenter = mutableListOf<Segment<UtbetalingData>>()
            utbetalingRepository.hent(behandlingRef).sortedBy { it.id }. forEach { utbetaling ->
                utbetaling.perioder.forEach { periode ->
                    segmenter.add(Segment<UtbetalingData>(periode.periode, UtbetalingData(
                        utbetalingRef = utbetaling.utbetalingRef,
                        beløp = periode.beløp,
                        fastsattDagsats = periode.fastsattDagsats,
                        utbetalingsdato = periode.utbetalingsdato
                    )))
                }
            }
            utbetalingTidslinje = utbetalingTidslinje.kombiner(Tidslinje<UtbetalingData>(segmenter), StandardSammenslåere.prioriterHøyreSideCrossJoin())
        }
        return utbetalingTidslinje
    }

}