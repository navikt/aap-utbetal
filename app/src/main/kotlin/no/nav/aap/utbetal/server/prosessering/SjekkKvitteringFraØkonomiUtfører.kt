package no.nav.aap.utbetal.server.prosessering

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.utbetal.utbetaling.UtbetalingRepository
import no.nav.aap.utbetaling.UtbetalingStatus

class SjekkKvitteringFraØkonomiUtfører(private val connection: DBConnection): JobbUtfører {

    override fun utfør(input: JobbInput) {
        //TODO: sjekk status for behandling gjennom helved-utbetaling
        val utbetalingId = input.parameter("utbetalingId").toLong()
        UtbetalingRepository(connection).oppdaterStatus(utbetalingId, UtbetalingStatus.BEKREFTET)
    }

    companion object: Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return SjekkKvitteringFraØkonomiUtfører(connection)
        }

        override fun type(): String {
            return "batch.sjekkKvitteringFraØkonomi"
        }

        override fun navn(): String {
            return "Sjekk kvittering fra økonomi"
        }

        override fun beskrivelse(): String {
            return "Sjekk kvittering fra økonomi"
        }
    }

}