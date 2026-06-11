package no.nav.aap.utbetal.utbetaling

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.utbetal.tilkjentytelse.UtbetalingStatusRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SjekkStatusForUtbetalingerService(private val connection: DBConnection) {

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    fun sjekkStatusForUtbetalinger() {
        //NB: Midlertidig logging av antall utbetalinger per status. Bedre overvåkning lages senere.
        val antallPerStatus = UtbetalingStatusRepository(connection).antallUtbetalingerPerStatus()
        log.info("Antall utbetalinger per status(nytt grensesnitt: $antallPerStatus")
    }

}