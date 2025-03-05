package no.nav.aap.utbetal.server.prosessering

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.utbetal.klienter.helved.UtbetalingKlient
import no.nav.aap.utbetal.utbetaling.UtbetalingRepository
import no.nav.aap.utbetaling.UtbetalingStatus
import java.util.UUID

class SjekkKvitteringFraØkonomiUtfører(private val connection: DBConnection): JobbUtfører {

    override fun utfør(input: JobbInput) {
        val utbetalingRefString = input.parameter("utbetalingRef")
        val utbetalingRef = UUID.fromString(utbetalingRefString)
        val status = UtbetalingKlient().hentStatus(utbetalingRef)
        //TODO: håndter status
        UtbetalingRepository(connection).oppdaterStatus(utbetalingRef, UtbetalingStatus.BEKREFTET)
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