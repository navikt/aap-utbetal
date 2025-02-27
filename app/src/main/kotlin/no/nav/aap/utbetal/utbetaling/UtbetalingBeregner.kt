package no.nav.aap.utbetal.utbetaling

import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.utbetal.felles.YtelseDetaljer
import no.nav.aap.utbetal.felles.finnHelger
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelse
import no.nav.aap.utbetaling.UtbetalingStatus
import no.nav.aap.utbetaling.UtbetalingsperiodeType
import java.time.LocalDateTime
import java.util.UUID

class UtbetalingBeregner {

    fun tilkjentYtelseTilUtbetaling(sakUtbetalingId: Long, nyTilkjentYtelse: TilkjentYtelse, forrigeTilkjentYtelse: TilkjentYtelse?, periode: Periode): Utbetaling {
        val klippetNyTilkjentYtelseTidslinje = klippPeriodeOgFjernHelger(nyTilkjentYtelse, periode)
        val klippetForrigeTilkjentYtelseTidslinje = if (forrigeTilkjentYtelse != null) klippPeriodeOgFjernHelger(forrigeTilkjentYtelse, periode) else Tidslinje<YtelseDetaljer>()

        // Konverter til utbetalingsperioder og legg på utbetalingsperiodeType
        val utbetalingerTidslinje = klippetForrigeTilkjentYtelseTidslinje.kombiner(klippetNyTilkjentYtelseTidslinje, prioriterHøyreSideCrossJoinMedEndring())
        val utbetalingsperioder = utbetalingerTidslinje.segmenter().map { it.verdi }
        return Utbetaling(
            saksnummer = nyTilkjentYtelse.saksnummer,
            behandlingsreferanse = nyTilkjentYtelse.behandlingsreferanse,
            sakUtbetalingId = sakUtbetalingId,
            tilkjentYtelseId = nyTilkjentYtelse.id!!,
            personIdent = nyTilkjentYtelse.personIdent,
            vedtakstidspunkt = nyTilkjentYtelse.vedtakstidspunkt,
            beslutterId = nyTilkjentYtelse.beslutterId,
            saksbehandlerId = nyTilkjentYtelse.saksbehandlerId,
            utbetalingOversendt = LocalDateTime.now(),
            utbetalingStatus = UtbetalingStatus.OPPRETTET,
            perioder = utbetalingsperioder,
        )
    }


    private fun klippPeriodeOgFjernHelger(tilkjentYtelse: TilkjentYtelse, periode: Periode): Tidslinje<YtelseDetaljer> {

        val helger = periode.finnHelger()
        val ytelseTidslinje = tilkjentYtelse.tilTidslinje()
        val klippetYtelseTidslinje = ytelseTidslinje.disjoint(periode)
        val helgerTidslinje = helger.tilTidslinje()
        return klippetYtelseTidslinje.kombiner(helgerTidslinje, StandardSammenslåere.minus())
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

    private fun prioriterHøyreSideCrossJoinMedEndring(): JoinStyle.OUTER_JOIN<YtelseDetaljer, YtelseDetaljer, Utbetalingsperiode> {
        return JoinStyle.OUTER_JOIN { periode, venstre, høyre ->
            if (venstre != null && høyre != null) {
                if (venstre == høyre) {
                    return@OUTER_JOIN Segment(
                        periode,
                        Utbetalingsperiode(
                            periode = periode,
                            beløp = høyre.verdi.redusertDagsats.tilUInt(),
                            fastsattDagsats = høyre.verdi.dagsats.tilUInt(),
                            utbetalingsperiodeType = UtbetalingsperiodeType.UENDRET
                        )
                    )
                }
                return@OUTER_JOIN Segment(
                    periode,
                    Utbetalingsperiode(
                        periode = periode,
                        beløp = høyre.verdi.redusertDagsats.tilUInt(),
                        fastsattDagsats = høyre.verdi.dagsats.tilUInt(),
                        utbetalingsperiodeType = UtbetalingsperiodeType.ENDRET
                    )
                )
            }
            if (høyre != null) {
                return@OUTER_JOIN Segment(
                    periode,
                    Utbetalingsperiode(
                        periode = periode,
                        beløp = høyre.verdi.redusertDagsats.tilUInt(),
                        fastsattDagsats = høyre.verdi.dagsats.tilUInt(),
                        utbetalingsperiodeType = UtbetalingsperiodeType.NY
                    )
                )
            } else {
                if (venstre == null)  {
                    return@OUTER_JOIN null
                } else {
                    throw IllegalStateException("Periode i forrige behandling finnes ikke i ny behandling: $periode")
                }
            }
        }
    }

    private fun Beløp.tilUInt() = verdi.toBigInteger().toInt().toUInt()

}