package no.nav.aap.utbetal.server.prosessering

import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.utbetal.felles.YtelseDetaljer
import no.nav.aap.utbetal.felles.finnHelger
import no.nav.aap.utbetal.klienter.helved.Utbetaling
import no.nav.aap.utbetal.klienter.helved.Utbetalingsperiode
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelse
import java.time.LocalDate
import java.time.LocalDateTime

class HelvedUtbetalingOppretter {

    fun opprettUtbetaling(utbetalingId: Long, tilkjentYtelse: TilkjentYtelse, periode: Periode): Utbetaling {
        val helger = periode.finnHelger()
        val ytelseTidslinje = tilkjentYtelse.tilTidslinje()
        val klippetYtelseTidslinje = ytelseTidslinje.disjoint(periode)
        val helgerTidslinje = helger.tilTidslinje()
        val klippetYtelseTidslinjeUtenHelger = klippetYtelseTidslinje.kombiner(helgerTidslinje, StandardSammenslåere.minus())

        return Utbetaling(
            sakId = tilkjentYtelse.saksnummer.toString(),
            behandlingId = utbetalingId.toString(),
            personident = tilkjentYtelse.personIdent,
            vedtakstidspunkt = tilkjentYtelse.vedtakstidspunkt,
            beslutterId = tilkjentYtelse.beslutterId,
            saksbehandlerId = tilkjentYtelse.saksbehandlerId,
            perioder = klippetYtelseTidslinjeUtenHelger.tilUtbetalingsperioder()
        )
    }

    private fun Tidslinje<YtelseDetaljer>.tilUtbetalingsperioder(): List<Utbetalingsperiode> {
        val utbetalingsperioder = mutableListOf<Utbetalingsperiode>()
        this.forEach { periode ->
            val detaljer = periode.verdi
            (periode.fom()..periode.tom()).iterator().forEach { dato ->
                utbetalingsperioder.add(
                    Utbetalingsperiode(
                        fom = dato,
                        tom = dato,
                        beløp = detaljer.redusertDagsats.toUint(),
                        fastsattDagsats = detaljer.dagsats.toUint(),
                    )
                )
            }
        }
        return utbetalingsperioder
    }

    private fun TilkjentYtelse.tilTidslinje() =
        Tidslinje(this.perioder.map { periode ->
            Segment<YtelseDetaljer>(
                periode.periode,
                periode.detaljer
            )
        })

    private fun List<Periode>.tilTidslinje() =
        Tidslinje(this.map { periode ->
            Segment<Unit>(
                periode,
                Unit
            )
        })

    private fun Beløp.toUint() = verdi.toInt().toUInt()

    private operator fun ClosedRange<LocalDate>.iterator() : Iterator<LocalDate>{
        return object: Iterator<LocalDate> {
            private var next = this@iterator.start
            private val finalElement = this@iterator.endInclusive
            private var hasNext = !next.isAfter(this@iterator.endInclusive)
            override fun hasNext(): Boolean = hasNext

            override fun next(): LocalDate {
                val value = next
                if(value == finalElement) {
                    hasNext = false
                }
                else {
                    next = next.plusDays(1)
                }
                return value
            }
        }
    }
}