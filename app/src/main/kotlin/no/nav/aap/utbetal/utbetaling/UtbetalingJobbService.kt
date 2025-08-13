package no.nav.aap.utbetal.utbetaling

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.utbetal.server.prosessering.OpprettUtbetalingUtfører
import no.nav.aap.utbetal.server.prosessering.OverførTilØkonomiJobbUtfører
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class UtbetalingJobbService(private val connection: DBConnection) {

    private val log: Logger = LoggerFactory.getLogger(UtbetalingJobbService::class.java)

    fun opprettUtbetalingJobb(sakUtbetalingId: Long, behandlingsreferanse: UUID) {
        log.info("Oppretter jobb for å overføre utbetaling til økonomi for behandlingsreferanse: $behandlingsreferanse")
        FlytJobbRepository(connection).leggTil(
            JobbInput(OpprettUtbetalingUtfører)
                .forSak(sakUtbetalingId)
                .medParameter("behandlingsreferanse", behandlingsreferanse.toString())
        )
    }

    fun overførUtbetalingJobb(sakUtbetaling: SakUtbetaling, utbetalingId: Long) {
        log.info("Oppretter jobb for å overføre utbetaling til økonomi for utbetalingId: $utbetalingId")
        FlytJobbRepository(connection).leggTil(
            JobbInput(OverførTilØkonomiJobbUtfører)
                .forSak(sakUtbetaling.id!!)
                .medParameter("utbetalingId", utbetalingId.toString())
        )
    }

}