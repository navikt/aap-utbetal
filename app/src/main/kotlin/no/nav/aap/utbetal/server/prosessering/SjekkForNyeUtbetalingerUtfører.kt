package no.nav.aap.utbetal.server.prosessering

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.utbetal.utbetaling.SakUtbetalingRepository
import no.nav.aap.utbetal.utbetaling.UtbetalingJobbService
import org.slf4j.LoggerFactory

class SjekkForNyeUtbetalingerUtfører(private val connection: DBConnection): JobbUtfører {

    private val log = LoggerFactory.getLogger(SjekkForNyeUtbetalingerUtfører::class.java)

    override fun utfør(input: JobbInput) {
        if (Miljø.erProd()) {
            log.info("SjekkForNyeUtbetalingerUtfører er deaktivert for produksjon inntil videre")
        } else {
            val sakOgBehandlingListe = SakUtbetalingRepository(connection).finnÅpneSakerOgSisteBehandling()
            log.info("Fant ${sakOgBehandlingListe.size} saker som må sjekkes for om nye utbetalinger skal dannes")
            val utbetalingJobbService = UtbetalingJobbService(connection)
            sakOgBehandlingListe.forEach { utbetalingJobbService.opprettUtbetalingJobb(it.saksnummer, it.behandlingRef) }
        }
    }

    companion object: Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return SjekkForNyeUtbetalingerUtfører(connection)
        }

        override fun type(): String {
            return "batch.sjekkForNyeUtbetalinger"
        }

        override fun navn(): String {
            return "Sjekk om det finnes nye utbetalinger"
        }

        override fun beskrivelse(): String {
            return "Sjekk om det finnes nye utbetalinger som skal overføres til økonomi"
        }

        /* -- NB: Denne skal ikke kjøres lengre. Fjernes fra koden etterhvert.

        override fun cron(): CronExpression? {
            //Kjøres kl 04:00.
            return CronExpression.create("0 0 4 * * *")
        }

        */
    }

}