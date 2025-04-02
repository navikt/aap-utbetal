package no.nav.aap.utbetal.utbetaling

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.httpklient.httpclient.error.IkkeFunnetException
import no.nav.aap.utbetal.klienter.helved.UtbetalingKlient
import no.nav.aap.utbetal.klienter.helved.UtbetalingStatus.FEILET_MOT_OPPDRAG
import no.nav.aap.utbetal.klienter.helved.UtbetalingStatus.IKKE_PÅBEGYNT
import no.nav.aap.utbetal.klienter.helved.UtbetalingStatus.OK
import no.nav.aap.utbetal.klienter.helved.UtbetalingStatus.OK_UTEN_UTBETALING
import no.nav.aap.utbetal.klienter.helved.UtbetalingStatus.SENDT_TIL_OPPDRAG
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class KvitteringService(private val connection: DBConnection) {

    private val log: Logger = LoggerFactory.getLogger(KvitteringService::class.java)

    fun sjekkKvittering(utbetaling: UtbetalingLight) {
        val utbetalingRepo = UtbetalingRepository(connection)
        val status = try {
            UtbetalingKlient().hentStatus(utbetaling.utbetalingRef)
        } catch (_: IkkeFunnetException) {
            null
        }
        when (status) {
            null -> {
                log.warn("Utbetaling ${utbetaling.utbetalingRef} ikke funnet i helved utbetaling")
                utbetalingRepo.oppdaterStatus(utbetaling.id, utbetaling.versjon, no.nav.aap.utbetaling.UtbetalingStatus.FEILET)
            }
            IKKE_PÅBEGYNT, SENDT_TIL_OPPDRAG -> {
                log.info("Utbetaling ${utbetaling.utbetalingRef} ikke behandlet: $status")
            }
            FEILET_MOT_OPPDRAG -> {
                log.info("Utbetaling ${utbetaling.utbetalingRef} feilet mot oppdrag")
                utbetalingRepo.oppdaterStatus(utbetaling.id, utbetaling.versjon, no.nav.aap.utbetaling.UtbetalingStatus.FEILET)
            }
            OK, OK_UTEN_UTBETALING -> {
                log.info("Utbetaling ${utbetaling.utbetalingRef} er BEKREFTET, og har status: $status")
                utbetalingRepo.oppdaterStatus(utbetaling.id, utbetaling.versjon, no.nav.aap.utbetaling.UtbetalingStatus.BEKREFTET)
            }
        }
    }
}