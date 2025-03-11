package no.nav.aap.utbetal.klienter.helved

import java.time.LocalDate

class HelvedUtbetalingOppretter {

    fun opprettUtbetaling(utbetaling: no.nav.aap.utbetal.utbetaling.Utbetaling): Utbetaling {

        return Utbetaling(
            sakId = utbetaling.saksnummer.toString(),
            behandlingId = utbetaling.behandlingsreferanse.toBase64(),
            personident = utbetaling.personIdent,
            vedtakstidspunkt = utbetaling.vedtakstidspunkt,
            beslutterId = utbetaling.beslutterId,
            saksbehandlerId = utbetaling.saksbehandlerId,
            perioder = utbetaling.perioder.tilUtbetalingsperioder()
        )
    }

    private fun List<no.nav.aap.utbetal.utbetaling.Utbetalingsperiode>.tilUtbetalingsperioder(): List<Utbetalingsperiode> {
        val utbetalingsperioder = mutableListOf<Utbetalingsperiode>()
        this.forEach { utbetalingsperiode ->
            val periode = utbetalingsperiode.periode
            (periode.fom..periode.tom).iterator().forEach { dato ->
                if (utbetalingsperiode.beløp > 0.toUInt()) {
                    utbetalingsperioder.add(
                        Utbetalingsperiode(
                            fom = dato,
                            tom = dato,
                            beløp = utbetalingsperiode.beløp,
                            fastsattDagsats = utbetalingsperiode.fastsattDagsats,
                        )
                    )
                }
            }
        }
        return utbetalingsperioder
    }

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