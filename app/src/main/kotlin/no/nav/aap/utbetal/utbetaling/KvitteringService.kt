package no.nav.aap.utbetal.utbetaling

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.httpklient.httpclient.error.IkkeFunnetException
import no.nav.aap.utbetal.klienter.helved.UtbetalingKlient
import no.nav.aap.utbetal.klienter.helved.UtbetalingStatus.FEILET_MOT_OPPDRAG
import no.nav.aap.utbetal.klienter.helved.UtbetalingStatus.IKKE_PÅBEGYNT
import no.nav.aap.utbetal.klienter.helved.UtbetalingStatus.OK
import no.nav.aap.utbetal.klienter.helved.UtbetalingStatus.OK_UTEN_UTBETALING
import no.nav.aap.utbetal.klienter.helved.UtbetalingStatus.SENDT_TIL_OPPDRAG
import no.nav.aap.utbetaling.UtbetalingStatus
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
        val utbetalingInfo = "uid = ${utbetaling.utbetalingRef}, saksnummer = ${utbetaling.saksnummer}, behandlingsref = ${utbetaling.behandlingsreferanse}"
        when (status) {
            null -> {
                log.warn("Helved utbetaling ikke funnet. $utbetalingInfo")
                utbetalingRepo.oppdaterStatus(utbetaling.id, utbetaling.versjon, UtbetalingStatus.FEILET)
            }
            IKKE_PÅBEGYNT -> {
                log.info("Utbetaling er ikke påbegynt. $utbetalingInfo")
            }
            SENDT_TIL_OPPDRAG -> {
                log.info("Utbetaling er sendt til oppdrag. $utbetalingInfo")
            }
            FEILET_MOT_OPPDRAG -> {
                if (utbetaling.utbetalingStatus == UtbetalingStatus.FEILET) {
                    log.info("Utbetaling feiler fortsatt mot oppdrag. $utbetalingInfo")
                } else {
                    log.warn("Utbetaling feilet mot oppdrag. $utbetalingInfo")
                    utbetalingRepo.oppdaterStatus(utbetaling.id, utbetaling.versjon, UtbetalingStatus.FEILET)
                }
            }
            OK, OK_UTEN_UTBETALING -> {
                log.info("Utbetaling  er BEKREFTET, og har status $status. $utbetalingInfo")
                utbetalingRepo.oppdaterStatus(utbetaling.id, utbetaling.versjon, UtbetalingStatus.BEKREFTET)
            }
        }
    }
}