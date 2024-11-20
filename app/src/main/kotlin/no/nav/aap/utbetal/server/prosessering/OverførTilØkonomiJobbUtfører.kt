package no.nav.aap.utbetal.server.prosessering

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.cron.CronExpression

class OverførTilØkonomiJobbUtfører(): JobbUtfører {
    override fun utfør(input: JobbInput) {
        println("==========================")
        println("SENDER TIL ØKONOMISYSTEMET")
        println("==========================")
    }

    companion object: Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return OverførTilØkonomiJobbUtfører()
        }

        override fun type(): String {
            return "batch.overførTilØkonomi"
        }

        override fun navn(): String {
            return "Overfør tilkjent ytelse til økonomi"
        }

        override fun beskrivelse(): String {
            return "Overfør tilkjent ytelse til økonomi"
        }

        override fun cron(): CronExpression? {
            return CronExpression.create("0 0 0 1,15 * *") //Midnatt den 1. og 15. hver måned.
        }
    }
}