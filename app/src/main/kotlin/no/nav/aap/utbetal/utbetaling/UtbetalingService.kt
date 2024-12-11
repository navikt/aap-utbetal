package no.nav.aap.utbetal.utbetaling

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.utbetal.server.prosessering.OverførTilØkonomiJobbUtfører
import no.nav.aap.utbetal.utbetalingsplan.SakUtbetalingRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Properties
import javax.sql.DataSource

class UtbetalingService(private val dataSource: DataSource) {

    private val log: Logger = LoggerFactory.getLogger(UtbetalingService::class.java)

    fun opprettUtbetalingsjobber() {
        dataSource.transaction() { connection ->
            val aktiveSaker = SakUtbetalingRepository(connection).finnAktiveSaker()
            aktiveSaker.forEach { sak ->
                val props = Properties()
                props["saksnummer"] = sak.saksnummer.toString()
                log.info("Oppretter utbetaling for saksnummer: ${sak.saksnummer}")
                FlytJobbRepository(connection).leggTil(
                    JobbInput(OverførTilØkonomiJobbUtfører)
                        .medProperties(props)
                        //.medNesteKjøring(nesteKjøreing) //TODO: skal den kjøre med en gang?
                )
            }
        }
    }

}