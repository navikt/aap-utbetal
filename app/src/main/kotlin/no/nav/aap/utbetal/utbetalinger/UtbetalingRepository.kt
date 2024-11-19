package no.nav.aap.utbetal.utbetalinger

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import java.util.UUID

data class Utbetaling(
    val behandlingsreferanse: UUID,
    val perioder: List<UtbetalingPeriode>,

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
    val barnetillegg: Beløp
)

class UtbetalingRepository(connection: DBConnection) {

    fun lagre(utbetaling: Utbetaling) {
        TODO()
    }

    fun hentUtbetalinger(saksnummer: String): List<Utbetaling> {
        TODO()
    }


}