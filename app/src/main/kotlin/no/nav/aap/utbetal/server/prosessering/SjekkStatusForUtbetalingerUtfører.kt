package no.nav.aap.utbetal.server.prosessering

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.cron.CronExpression
import no.nav.aap.utbetal.utbetaling.SjekkStatusForUtbetalingerService

class SjekkStatusForUtbetalingerUtfører(private val connection: DBConnection): JobbUtfører {

    override fun utfør(input: JobbInput) {
        SjekkStatusForUtbetalingerService(connection).sjekkStatusForUtbetalinger()
    }

    companion object: Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return SjekkStatusForUtbetalingerUtfører(connection)
        }

        override fun type(): String {
            return "batch.sjekkStatusForUtbetalinger"
        }

        override fun navn(): String {
            return "Sjekk status for utbetalinger"
        }

        override fun beskrivelse(): String {
            return "Sjekk status for utbetalinger"
        }

        override fun cron(): CronExpression {
            //Hvert 10. minutt hele døgnet
            return CronExpression.create("0 0/10 * * * *")
        }
    }
}