package no.nav.aap.utbetal.server.prosessering

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.utbetal.utbetaling.UtbetalingJobbService
import no.nav.aap.utbetal.utbetaling.UtbetalingService
import java.util.*

class OpprettUtbetalingUtfører(private val connection: DBConnection): JobbUtfører {
    override fun utfør(input: JobbInput) {
        val behandlingsreferanse = input.parameter("behandlingsreferanse")

        val utbetalinger = UtbetalingService(connection).opprettUtbetalinger(
            behandlingsreferanse = UUID.fromString(behandlingsreferanse)
        )

        val utbetalingJobbService = UtbetalingJobbService(connection)
        utbetalinger.alle()
            .forEach { utbetaling -> utbetalingJobbService.overførUtbetalingJobb(utbetaling.id!!) }
    }

    companion object: Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return OpprettUtbetalingUtfører(connection)
        }

        override fun type(): String {
            return "batch.opprettUtbetaling"
        }

        override fun navn(): String {
            return "Opprett utbetaling"
        }

        override fun beskrivelse(): String {
            return "Opprett utbetaling"
        }

    }

}