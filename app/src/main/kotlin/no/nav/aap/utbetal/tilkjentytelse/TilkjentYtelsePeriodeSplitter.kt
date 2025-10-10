package no.nav.aap.utbetal.tilkjentytelse

import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.utbetal.felles.YtelseDetaljer
import no.nav.aap.utbetal.trekk.TrekkPostering
import java.math.BigDecimal

object TilkjentYtelsePeriodeSplitter {

    fun splitt(tilkjentYtelse: TilkjentYtelse, trekkPosteringer: List<TrekkPostering>): TilkjentYtelse {
        if (trekkPosteringer.isEmpty()) return tilkjentYtelse
        val trekkPosteringerTidslinje = byggTrekkPosteringTidslinje(trekkPosteringer)
        val perioderEtterTrekk = tilkjentYtelse.perioder.flatMap { tilkjentYtelsePeriode ->
            val tyPeriodeTidslinje =
                Tidslinje(listOf(Segment(tilkjentYtelsePeriode.periode, tilkjentYtelsePeriode.detaljer)))
            tyPeriodeTidslinje
                .kombiner(trekkPosteringerTidslinje, prioriterHøyreDersomVenstreSideFinnes())
                .segmenter()
                .map { TilkjentYtelsePeriode(periode = it.periode, detaljer = it.verdi) }
        }
        return tilkjentYtelse.copy(perioder = perioderEtterTrekk)
    }

    private fun prioriterHøyreDersomVenstreSideFinnes(): JoinStyle.OUTER_JOIN<YtelseDetaljer, TrekkPostering, YtelseDetaljer> {
        return JoinStyle.OUTER_JOIN { periode, venstre, høyre ->
            if (høyre != null && venstre != null) {
                val nyRedusertDagsats = venstre.verdi.redusertDagsats.minus(Beløp(høyre.verdi.beløp))
                if (nyRedusertDagsats.verdi() < BigDecimal.ZERO) {
                    throw IllegalStateException("Redusert dagsats kan ikke være negativ (redusert dagsats: $nyRedusertDagsats)")
                }
                return@OUTER_JOIN Segment(
                    periode = periode,
                    verdi = venstre.verdi.copy(
                        redusertDagsats = nyRedusertDagsats,
                        trekkPosteringId = høyre.verdi.id,
                    )
                )
            }
            if (venstre == null) return@OUTER_JOIN null
            Segment(periode, venstre.verdi)
        }
    }

    private fun byggTrekkPosteringTidslinje(trekkPosteringer: List<TrekkPostering>): Tidslinje<TrekkPostering> {
        val trekkPosteringSegmenter = trekkPosteringer.map { Segment(Periode(it.dato, it.dato), it) }
        return Tidslinje(trekkPosteringSegmenter)
    }

}