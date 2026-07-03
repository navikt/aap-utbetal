package no.nav.aap.utbetal.server.prosessering

import no.bekk.bekkopen.date.NorwegianDateUtil.isWorkingDay
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.cron.CronExpression
import no.nav.aap.utbetal.utbetaling.KvitteringService
import no.nav.aap.utbetal.utbetaling.UtbetalingLight
import no.nav.aap.utbetal.utbetaling.UtbetalingRepository
import no.nav.aap.utbetaling.UtbetalingStatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*
import kotlin.time.measureTime

class SjekkKvitteringFraØkonomiUtfører(private val connection: DBConnection): JobbUtfører {

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val utbetalingerSomManglerKvitteringer = UtbetalingRepository(connection).hentUtbetalingerSomManglerKvittering()

        loggForsinkelserOgFeil(utbetalingerSomManglerKvitteringer)

        val kvitteringService = KvitteringService(connection)
        val tid = measureTime {
            utbetalingerSomManglerKvitteringer.forEach { kvitteringService.sjekkKvittering(it) }
        }
        log.info("Henting av kvittering for ${utbetalingerSomManglerKvitteringer.size} utbetalinger tok $tid")
    }

    private fun loggForsinkelserOgFeil(ukvitterteUtbetalinger: List<UtbetalingLight>) {
        val antallForsinket = ukvitterteUtbetalinger.count {it.utbetalingStatus == UtbetalingStatus.SENDT}
        val antallFeilet = ukvitterteUtbetalinger.count {it.utbetalingStatus == UtbetalingStatus.FEILET}
        
        val for10MinutterSiden = LocalDateTime.now().minusMinutes(10)
        val antallForsinketMerEnn10Minutter = ukvitterteUtbetalinger.count {it.utbetalingOpprettet < for10MinutterSiden  }
        
        if (antallForsinketMerEnn10Minutter > 0 && erForsinkelsenIVakttid()) {
            log.error("Mangler kvitteringer på $antallForsinket utbetalinger. Antall som er mer enn 10 minutter gamle: $antallForsinketMerEnn10Minutter")
        } else {
            log.info("Mangler kvitteringer på $antallForsinket utbetalinger. Antall som er mer enn 10 minutter gamle: $antallForsinketMerEnn10Minutter")
        }
        if (antallFeilet > 0) {
            val saksnummereForFeilet = ukvitterteUtbetalinger.filter { it.utbetalingStatus == UtbetalingStatus.FEILET }.map { it.saksnummer }
            log.error("Feilet status på $antallFeilet utbetalinger. Rapporter saksnummer(e) til #team-hel-ved. Saker: $saksnummereForFeilet")
        } else {
            log.info("Feilet status på $antallFeilet utbetalinger")
        }
    }

    /* Det er normalt med kvitteringsforsinkelser utenom åpningstiden til NAVs økonomisystem
    *  Åpningstiden er 06:00 til 21:00, men vi setter kun error level i vakt-tid 9 til 15. Vi tar dermed
    *  også høyde for etterslepet fra 6 til rundt 9 i kvitteringsleveransen fra Økonomi hver morgen.
    * */
    private fun erForsinkelsenIVakttid(): Boolean {
        val idag = Date()
        val klokkeslett = LocalTime.now()
        val start = LocalTime.of(9, 0)
        val stopp = LocalTime.of(15, 0)
        return isWorkingDay(idag) && klokkeslett.isAfter(start) && klokkeslett.isBefore(stopp)
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

        override fun cron(): CronExpression {
            //Hvert 10. minutt hele døgnet
            return CronExpression.create("0 0/10 * * * *")
        }
    }

}