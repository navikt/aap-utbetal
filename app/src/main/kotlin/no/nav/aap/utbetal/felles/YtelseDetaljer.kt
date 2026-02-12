package no.nav.aap.utbetal.felles

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import java.time.LocalDate

data class YtelseDetaljer(
    val redusertDagsats: Beløp,
    val gradering: Prosent,
    val dagsats: Beløp,
    @Deprecated("Denne er alltid lik dagsats fra behandlingsflyt.")
    val grunnlag: Beløp,
    val grunnlagsfaktor: GUnit,
    val grunnbeløp: Beløp,
    val antallBarn: Int,
    val barnetilleggsats: Beløp,
    val barnetillegg: Beløp,
    val utbetalingsdato: LocalDate,
    val trekkPosteringId: Long? = null,
    val meldeperiode: Periode?,
)