package no.nav.aap.utbetal.utbetalingsplan

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import java.util.UUID


class UtbetalingsplanRepository(connection: DBConnection) {

    fun lagre(utbetalingsplan: Utbetalingsplan) {
        TODO()
    }

    fun hentUtbetalingsplan(behandlingsreferanse: UUID): Utbetalingsplan? {
        TODO()
    }

}

