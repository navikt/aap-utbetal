package no.nav.aap.utbetal.utbetalingsplan

import no.nav.aap.komponenter.dbconnect.DBConnection
import java.util.UUID


class UtbetalingsplanRepository(connection: DBConnection) {

    fun lagre(utbetalingsplan: Utbetalingsplan) {
        TODO()
    }

    fun hent(behandlingsreferanse: UUID): Utbetalingsplan? {
        TODO()
    }

    //TODO: metode for utbetalingsplan

}

