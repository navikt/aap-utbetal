package no.nav.aap.utbetal.felles

import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import java.time.LocalDate

private val DATO_FOR_BARNETILLEGG_I_DAGSATS = LocalDate.parse("2026-09-28")

data class YtelseDetaljer(
    val redusertDagsats: Beløp,
    val gradering: Prosent,
    val dagsats: Beløp,
    val grunnlagsfaktor: GUnit,
    val grunnbeløp: Beløp,
    val antallBarn: Int,
    val barnepensjonDagsats: Beløp,
    val barnetilleggsats: Beløp,
    val barnetillegg: Beløp,
    val utbetalingsdato: LocalDate,
    val trekkPosteringId: Long? = null,
    val meldeperiode: Periode?,
) {

    /**
     * Dagsats som skal brukes for å finne rekke skattekort. Skal brukes i feltet fastsattDagsats i utbetalingsperioden.
     */
    fun dagsatsMedBarnetillegg(periode: Periode): Beløp {
        if (periode.fom.isBefore(DATO_FOR_BARNETILLEGG_I_DAGSATS) || Miljø.erProd()) {
            // Behold gammel beregning av dagsats for gamle perioder.
            return dagsats
        }
        return dagsats.pluss(barnetillegg)
    }

}