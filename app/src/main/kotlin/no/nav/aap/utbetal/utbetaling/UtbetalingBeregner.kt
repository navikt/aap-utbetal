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

data class UtbetalingsperiodeMedReferanse(
    val utbetalingRef: UUID,
    val utbetalingsperiode: Utbetalingsperiode
)

data class Utbetalinger(
    val endringUtbetalinger: List<Utbetaling>,
    val nyeUtbetalinger: List<Utbetaling>,
) {
    fun alle(): List<Utbetaling> = endringUtbetalinger + nyeUtbetalinger
}

class UtbetalingBeregner {

    fun tilkjentYtelseTilUtbetaling(sakUtbetalingId: Long, nyTilkjentYtelse: TilkjentYtelse, tidligereUtbetalingerTidslinje: Tidslinje<UtbetalingData>): Utbetalinger {
        val periodeSomSkalSendes = finnPeriodeSomSkalSendes(nyTilkjentYtelse)
        val nyUtbetalingRef = UUID.randomUUID()
        val utbetalingsperioder = if (periodeSomSkalSendes == null) {
            listOf()
        } else {
            val klippetNyTilkjentYtelseTidslinje = klippPeriodeOgFjernHelger(nyTilkjentYtelse, periodeSomSkalSendes)

            // Konverter til utbetalingsperioder og legg på utbetalingsperiodeType
            val utbetalingerTidslinje = tidligereUtbetalingerTidslinje.kombiner(klippetNyTilkjentYtelseTidslinje, prioriterHøyreSideCrossJoinMedEndring(nyUtbetalingRef))
            utbetalingerTidslinje.segmenter().map { it.verdi }
        }
        val nyeUtbetalingsperioder = utbetalingsperioder.filter {it.utbetalingRef == nyUtbetalingRef}.map {it.utbetalingsperiode}.filter {it.beløp > 0.toUInt()}
        val utbetalingerMedNyePerioder =  nyeUtbetalingsperioder.splittPerBeløp().map { (utbetalingRef, utbetalingsperioder) ->
            Utbetaling(
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
                utbetalingRef = utbetalingRef
            )
        }
        return Utbetalinger(
            endringUtbetalinger = utbetalingsperioder.lagUtbetalingerForEndringer(sakUtbetalingId, nyTilkjentYtelse),
            nyeUtbetalinger = utbetalingerMedNyePerioder
        )
    }

    private fun List<Utbetalingsperiode>.splittPerBeløp(): Map<UUID, List<Utbetalingsperiode>> {
        val perBeløp = groupBy {it.beløp}
        return perBeløp.entries.associate {UUID.randomUUID() to it.value.sortedBy { it.periode.fom }}
    }

    private fun List<UtbetalingsperiodeMedReferanse>.lagUtbetalingerForEndringer(sakUtbetalingId: Long, nyTilkjentYtelse: TilkjentYtelse): List<Utbetaling> {
        val utbetalingRefEndringer = finnUtbetalingerSomSkalSendesSomEndring()
        return utbetalingRefEndringer.map { utbetalingRef ->
            val utbetalingsperioder = this.filter {it.utbetalingRef == utbetalingRef} .map {it.utbetalingsperiode}.filter {it.beløp > 0.toUInt()}
            Utbetaling(
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
                utbetalingRef = utbetalingRef
            )
        }
    }

    private fun List<UtbetalingsperiodeMedReferanse>.finnUtbetalingerSomSkalSendesSomEndring(): Set<UUID> {
        return this
            .filter {it.utbetalingsperiode.utbetalingsperiodeType == UtbetalingsperiodeType.ENDRET }
            .map { utbetaling -> utbetaling.utbetalingRef }
            .toSet()
    }

    private fun finnPeriodeSomSkalSendes(nyTilkjentYtelse: TilkjentYtelse): Periode? {
        val sisteUtbetalingsdato = nyTilkjentYtelse.vedtakstidspunkt.toLocalDate()
        val min = nyTilkjentYtelse.perioder.minOfOrNull { it.periode.fom }
        val max = nyTilkjentYtelse.perioder.filter { it.periode.tom <= sisteUtbetalingsdato }.maxOfOrNull { it.periode.tom }
        return if (min == null || max == null || min.isAfter(max)) {
            null
        } else {
            Periode(min, max)
        }
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

    private fun prioriterHøyreSideCrossJoinMedEndring(nyUtbetalingRef: UUID): JoinStyle.OUTER_JOIN<UtbetalingData, YtelseDetaljer, UtbetalingsperiodeMedReferanse> {
        return JoinStyle.OUTER_JOIN { periode, venstre, høyre ->
            if (venstre != null && høyre != null) {
                if (sammeBeløp(venstre.verdi, høyre.verdi)) {
                    return@OUTER_JOIN Segment(
                        periode,
                        UtbetalingsperiodeMedReferanse(
                            utbetalingRef = venstre.verdi.utbetalingRef,
                            utbetalingsperiode = Utbetalingsperiode(
                                periode = periode,
                                beløp = høyre.verdi.redusertDagsats.tilUInt(),
                                fastsattDagsats = høyre.verdi.dagsats.tilUInt(),
                                utbetalingsperiodeType = UtbetalingsperiodeType.UENDRET,
                                utbetalingsdato = høyre.verdi.utbetalingsdato
                            )
                        )
                    )
                }
                return@OUTER_JOIN Segment(
                    periode,
                    UtbetalingsperiodeMedReferanse(
                        utbetalingRef = venstre.verdi.utbetalingRef,
                        utbetalingsperiode = Utbetalingsperiode(
                            periode = periode,
                            beløp = høyre.verdi.redusertDagsats.tilUInt(),
                            fastsattDagsats = høyre.verdi.dagsats.tilUInt(),
                            utbetalingsperiodeType = UtbetalingsperiodeType.ENDRET,
                            utbetalingsdato = høyre.verdi.utbetalingsdato
                        )
                    )

                )
            }
            if (høyre != null) {
                return@OUTER_JOIN Segment(
                    periode,
                    UtbetalingsperiodeMedReferanse(
                        utbetalingRef = nyUtbetalingRef,
                        utbetalingsperiode =  Utbetalingsperiode(
                            periode = periode,
                            beløp = høyre.verdi.redusertDagsats.tilUInt(),
                            fastsattDagsats = høyre.verdi.dagsats.tilUInt(),
                            utbetalingsperiodeType = UtbetalingsperiodeType.NY,
                            utbetalingsdato = høyre.verdi.utbetalingsdato
                        )
                    )

                )
            } else {
                return@OUTER_JOIN null
            }
        }
    }

    private fun sammeBeløp(utbetalingData: UtbetalingData, ytelseDetaljer: YtelseDetaljer): Boolean {
        return utbetalingData.beløp == ytelseDetaljer.redusertDagsats.tilUInt() &&
                utbetalingData.fastsattDagsats == ytelseDetaljer.redusertDagsats.tilUInt()
    }


    private fun Beløp.tilUInt() = verdi.toBigInteger().toInt().toUInt()

}