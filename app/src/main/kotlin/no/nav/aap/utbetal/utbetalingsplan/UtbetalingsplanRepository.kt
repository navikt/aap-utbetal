package no.nav.aap.utbetal.utbetalingsplan

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import java.util.UUID

data class Utbetalingsplan(
    val behandlingsreferanse: UUID,
    val forrigeBehandlingsreferanse: UUID? = null,
    val perioder: List<UtbetalingPeriode>
)

data class UtbetalingPeriode(
    val periode: Periode,
    val dagsats: Beløp,
    val gradering: Prosent,
    val grunnlag: Beløp,
    val grunnlagsfaktor: GUnit,
    val grunnbeløp: Beløp,
    val antallBarn: Int,
    val barnetilleggsats: Beløp,
    val barnetillegg: Beløp,
    val endretSidenForrige: Boolean = false
)

class UtbetalingsplanRepository(connection: DBConnection) {

    fun lagre(utbetalingsplan: Utbetalingsplan) {
        TODO()
    }

    fun hentUtbetalingsplan(behandlingsreferanse: UUID): Utbetalingsplan? {
        TODO()
    }


}