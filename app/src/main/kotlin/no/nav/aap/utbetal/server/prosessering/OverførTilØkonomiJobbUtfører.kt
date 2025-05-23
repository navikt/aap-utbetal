package no.nav.aap.utbetal.server.prosessering

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.utbetal.klienter.helved.HelvedUtbetalingOppretter
import no.nav.aap.utbetal.klienter.helved.UtbetalingKlient
import no.nav.aap.utbetal.utbetaling.Utbetaling
import no.nav.aap.utbetal.utbetaling.UtbetalingJobbService
import no.nav.aap.utbetal.utbetaling.UtbetalingRepository
import no.nav.aap.utbetaling.UtbetalingStatus
import no.nav.aap.utbetaling.UtbetalingsperiodeType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class OverførTilØkonomiJobbUtfører(private val connection: DBConnection): JobbUtfører {

    private val log: Logger = LoggerFactory.getLogger(UtbetalingJobbService::class.java)

    override fun utfør(input: JobbInput) {
        val utbetalingRepo = UtbetalingRepository(connection)
        val utbetalingId = input.parameter("utbetalingId").toLong()
        val utbetaling = utbetalingRepo.hentUtbetaling(utbetalingId)
        val helvedUtbetaling = HelvedUtbetalingOppretter().opprettUtbetaling(utbetaling)

        if (utbetaling.harNyePerioder()) {
            log.info("Overfører nye periode til økonomi for utbetalingId: $utbetalingId")
            UtbetalingKlient().iverksettNy(utbetaling.utbetalingRef, helvedUtbetaling)
        } else {
            if (utbetaling.perioder.isEmpty()) {
                log.info("Opphør av utbetalingId: $utbetalingId")
                val helvedUtbetaling = UtbetalingKlient().hentUtbetaling(utbetaling.utbetalingRef)
                UtbetalingKlient().opphør(utbetaling.utbetalingRef, helvedUtbetaling)
            } else {
                log.info("Overfører endringer til økonomi for utbetalingId: $utbetalingId")
                UtbetalingKlient().iverksettEndring(utbetaling.utbetalingRef, helvedUtbetaling)
            }
        }
        utbetalingRepo.oppdaterStatus(utbetalingId, utbetaling.versjon, UtbetalingStatus.SENDT)
    }

    private fun Utbetaling.harNyePerioder() =
        perioder.any { it.utbetalingsperiodeType == UtbetalingsperiodeType.NY }


    companion object: Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return OverførTilØkonomiJobbUtfører(connection)
        }

        override fun type(): String {
            return "batch.overførTilØkonomi"
        }

        override fun navn(): String {
            return "Overfør til økonomi"
        }

        override fun beskrivelse(): String {
            return "Overfør til økonomi"
        }
    }
}