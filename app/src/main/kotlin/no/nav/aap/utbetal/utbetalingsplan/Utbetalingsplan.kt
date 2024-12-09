package no.nav.aap.utbetal.utbetalingsplan

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import java.util.UUID

data class Utbetalingsplan(
    val behandlingsreferanse: UUID,
    val forrigeBehandlingsreferanse: UUID? = null,
    val perioder: List<Utbetalingsperiode>
)

enum class UtbetalingsperiodeType {
    NY,
    ENDRET,
    UENDRET
}

data class Utbetalingsperiode(val periode: Periode, val utbetaling: Utbetaling, val utbetalingsperiodeType: UtbetalingsperiodeType)

data class Utbetaling(
    val redusertDagsats: Beløp,
    val dagsats: Beløp,
    val gradering: Prosent,
    val grunnlag: Beløp,
    val grunnlagsfaktor: GUnit,
    val grunnbeløp: Beløp,
    val antallBarn: Int,
    val barnetilleggsats: Beløp,
    val barnetillegg: Beløp,
)
