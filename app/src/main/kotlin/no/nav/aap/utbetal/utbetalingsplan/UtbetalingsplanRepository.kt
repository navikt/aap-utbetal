package no.nav.aap.utbetal.utbetalingsplan

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import java.util.UUID

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

sealed interface Utbetalingsperiode {
    data class UendretPeriode(val periode: Periode, val utbetaling: Utbetaling) : Utbetalingsperiode
    data class NyPeriode(val periode: Periode, val utbetaling: Utbetaling) : Utbetalingsperiode
    data class EndretPeriode(val periode: Periode, val tidligereUtbetaling: Utbetaling, val nyUtbetaling: Utbetaling) : Utbetalingsperiode
}

data class Utbetalingsplan(
    val behandlingsreferanse: UUID,
    val forrigeBehandlingsreferanse: UUID? = null,
    val perioder: List<Utbetalingsperiode>
)

class UtbetalingsplanRepository(connection: DBConnection) {

    fun lagre(utbetalingsplan: Utbetalingsplan) {
        TODO()
    }

    fun hentUtbetalingsplan(behandlingsreferanse: UUID): Utbetalingsplan? {
        TODO()
    }

}