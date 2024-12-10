package no.nav.aap.utbetal.utbetaling

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.utbetal.server.prosessering.OverførTilØkonomiJobbUtfører
import no.nav.aap.utbetal.utbetalingsplan.SakUtbetalingRepository
import java.time.LocalDateTime
import java.util.Properties
import javax.sql.DataSource

class UtbetalingService(private val dataSource: DataSource) {

    fun opprettUtbetalingTasker(nesteKjøreing: LocalDateTime) {
        dataSource.transaction() { connection ->
            val aktiveSaker = SakUtbetalingRepository(connection).finnAktiveSaker()
            aktiveSaker.forEach { sak ->
                val props = Properties()
                props["saksnummer"] = sak.saksnummer
                FlytJobbRepository(connection).leggTil(
                    JobbInput(OverførTilØkonomiJobbUtfører)
                        .medProperties(props)
                        .medNesteKjøring(nesteKjøreing) //TODO: skal den kjøre med en gang?
                )
            }
        }
    }

}