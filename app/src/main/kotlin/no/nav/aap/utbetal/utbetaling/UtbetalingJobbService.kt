package no.nav.aap.utbetal.utbetaling

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.utbetal.server.prosessering.OpprettUtbetalingUtfører
import no.nav.aap.utbetal.server.prosessering.OverførTilØkonomiJobbUtfører
import no.nav.aap.utbetal.server.prosessering.SjekkKvitteringFraØkonomiUtfører
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class UtbetalingJobbService(private val connection: DBConnection) {

    private val log: Logger = LoggerFactory.getLogger(UtbetalingJobbService::class.java)

    fun opprettUtbetalingJobb(saksnummer: String, behandlingsreferanse: UUID) {
        log.info("Oppretter jobb for å overføre utbetaling til økonomi for behandlingsreferanse: $behandlingsreferanse")
        FlytJobbRepository(connection).leggTil(
            JobbInput(OpprettUtbetalingUtfører)
                .medParameter("saksnummer", saksnummer)
                .medParameter("behandlingsreferanse", behandlingsreferanse.toString())
        )
    }

    fun overførUtbetalingJobb(utbetalingId: Long) {
        log.info("Oppretter jobb for å overføre utbetaling til økonomi for utbetalingId: $utbetalingId")
        FlytJobbRepository(connection).leggTil(
            JobbInput(OverførTilØkonomiJobbUtfører).medParameter("utbetalingId", utbetalingId.toString())
        )
    }


    fun opprettSjekkKvitteringJobb(utbetalingRef: UUID) {
        log.info("Oppretter jobb for å sjekke status på utbetaling: $utbetalingRef")
        FlytJobbRepository(connection).leggTil(
            JobbInput(SjekkKvitteringFraØkonomiUtfører)
                .medParameter("utbetalingRef", utbetalingRef.toString())
        )
    }

}