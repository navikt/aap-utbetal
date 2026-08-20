package no.nav.aap.utbetal.felles

import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import java.time.LocalDate

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
     * Dagsats som skal brukes for å finne rekke skattekort. Skal brukes i feltet fastsattDagsats u utbetalingsperioden.
     */
    fun dagsatsMedBarnetillegg(): Beløp {
        if (Miljø.erProd()) {
            //Behold dagens logikk til vi har sjekket at det virker i testmiljø.
            return dagsats
        }
        return dagsats.pluss(barnetillegg)
    }

}