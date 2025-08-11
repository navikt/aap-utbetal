package no.nav.aap.utbetal.server.prosessering

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.cron.CronExpression
import no.nav.aap.utbetal.utbetaling.KvitteringService
import no.nav.aap.utbetal.utbetaling.UtbetalingRepository
import no.nav.aap.utbetaling.UtbetalingStatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SjekkKvitteringFraØkonomiUtfører(private val connection: DBConnection): JobbUtfører {

    private val log: Logger = LoggerFactory.getLogger(SjekkKvitteringFraØkonomiUtfører::class.java)

    override fun utfør(input: JobbInput) {
        val utbetalingerSomManglerKvitteringer = UtbetalingRepository(connection).hentUtbetalingerSomManglerKvittering()
        val sendtMenManglerKvittering = utbetalingerSomManglerKvitteringer.count {it.utbetalingStatus == UtbetalingStatus.SENDT}
        val feiletStatus = utbetalingerSomManglerKvitteringer.count {it.utbetalingStatus == UtbetalingStatus.FEILET}
        log.info(("Miljø: ${Miljø.er()}"))
        log.info("Mangler kvitteringer på $sendtMenManglerKvittering utbetalinger")
        log.info("Feilet status på $feiletStatus utbetalinger")
        val kvitteringService = KvitteringService(connection)
        utbetalingerSomManglerKvitteringer.forEach { kvitteringService.sjekkKvittering(it) }
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