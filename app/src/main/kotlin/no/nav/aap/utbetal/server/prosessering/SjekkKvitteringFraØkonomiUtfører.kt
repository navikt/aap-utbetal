package no.nav.aap.utbetal.server.prosessering

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.cron.CronExpression
import no.nav.aap.utbetal.klienter.helved.UtbetalingKlient
import no.nav.aap.utbetal.klienter.helved.UtbetalingStatus.FEILET_MOT_OPPDRAG
import no.nav.aap.utbetal.klienter.helved.UtbetalingStatus.IKKE_PÅBEGYNT
import no.nav.aap.utbetal.klienter.helved.UtbetalingStatus.OK
import no.nav.aap.utbetal.klienter.helved.UtbetalingStatus.OK_UTEN_UTBETALING
import no.nav.aap.utbetal.klienter.helved.UtbetalingStatus.SENDT_TIL_OPPDRAG
import no.nav.aap.utbetal.utbetaling.UtbetalingRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SjekkKvitteringFraØkonomiUtfører(private val connection: DBConnection): JobbUtfører {

    private val log: Logger = LoggerFactory.getLogger(SjekkKvitteringFraØkonomiUtfører::class.java)

    override fun utfør(input: JobbInput) {
        val utbetalingRepo = UtbetalingRepository(connection)
        val sendteUtbetalinger = utbetalingRepo.hentAlleSendteUtbetalinger()
        log.info("Fant ${sendteUtbetalinger.size} utbetalinger som mangler kvittering")
        sendteUtbetalinger.forEach { utbetaling ->
            val status = UtbetalingKlient().hentStatus(utbetaling.utbetalingRef)
            when (status) {
                IKKE_PÅBEGYNT, SENDT_TIL_OPPDRAG -> {
                    log.info("Utbetaling ${utbetaling.utbetalingRef} ikke behandlet: $status")
                }
                FEILET_MOT_OPPDRAG -> {
                    log.info("Utbetaling ${utbetaling.utbetalingRef} feilet mot oppdrag")
                    utbetalingRepo.oppdaterStatus(utbetaling.id, no.nav.aap.utbetaling.UtbetalingStatus.FEILET)
                }
                OK, OK_UTEN_UTBETALING -> {
                    log.info("Utbetaling ${utbetaling.utbetalingRef} er ok, og har status: $status")
                    utbetalingRepo.oppdaterStatus(utbetaling.id, no.nav.aap.utbetaling.UtbetalingStatus.BEKREFTET)
                }
            }
        }

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

        override fun cron(): CronExpression? {
            //Hvert 10. minutt hele døgnet
            return CronExpression.create("0 0/10 * * * *")
        }
    }

}