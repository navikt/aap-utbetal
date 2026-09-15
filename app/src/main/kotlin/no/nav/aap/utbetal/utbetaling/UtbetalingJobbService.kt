package no.nav.aap.utbetal.utbetaling

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.utbetal.kodeverk.AvventÅrsak
import no.nav.aap.utbetal.server.prosessering.gammelt_grensesnitt.OpprettUtbetalingUtfører
import no.nav.aap.utbetal.server.prosessering.nytt_grensesnitt.OpprettUtbetalingsmeldingUtfører
import no.nav.aap.utbetal.server.prosessering.gammelt_grensesnitt.OverførTilØkonomiJobbUtfører
import no.nav.aap.utbetal.server.prosessering.nytt_grensesnitt.SendSlettAvventPeriodeUtfører
import no.nav.aap.utbetal.server.prosessering.nytt_grensesnitt.SendUtbetalingsmeldingUtfører
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class UtbetalingJobbService(private val connection: DBConnection) {

    private val log: Logger = LoggerFactory.getLogger(javaClass)
    private val utsettUtbetalingAntallSekunder = 10L

    fun opprettUtbetalingJobb(sakUtbetalingId: Long, behandlingsreferanse: UUID) {
        log.info("Oppretter jobb for å overføre utbetaling til økonomi for behandlingsreferanse: $behandlingsreferanse")
        FlytJobbRepository(connection).leggTil(
            JobbInput(OpprettUtbetalingUtfører)
                .forSak(sakUtbetalingId)
                .medParameter("behandlingsreferanse", behandlingsreferanse.toString())
        )
    }

    fun overførUtbetalingJobb(sakUtbetaling: SakUtbetaling, utbetalingId: Long, overførUtbetalingTidspunkt: LocalDateTime? = null) {
        log.info("Oppretter jobb for å overføre utbetaling til økonomi for utbetalingId: $utbetalingId")
        val jobbInput = JobbInput(OverførTilØkonomiJobbUtfører)
            .forSak(sakUtbetaling.id!!)
            .medParameter("utbetalingId", utbetalingId.toString())
        if (overførUtbetalingTidspunkt != null) {
            jobbInput.medNesteKjøring(overførUtbetalingTidspunkt)
        }
        FlytJobbRepository(connection).leggTil(jobbInput)
    }

    //
    // Jobber for nytt grensesnitt
    //

    fun opprettUtbetalingsmelding(sakUtbetalingId: Long, behandlingsreferanse: UUID) {
        log.info("Oppretter jobb for å opprette utbetalingsmelding, og starte jobb for å sende den til Utsjekk.")
        FlytJobbRepository(connection).leggTil(
            JobbInput(OpprettUtbetalingsmeldingUtfører)
                .forSak(sakUtbetalingId)
                .medParameter("behandlingsreferanse", behandlingsreferanse.toString())
        )
    }

    fun sendSlettAvventPeriode(
        tilkjentYtelseId: Long,
        sakUtbetalingId: Long,
        saksnummer: Saksnummer,
        referanse: UUID,
        personIdent: String,
        fom: LocalDate,
        tom: LocalDate,
        overføres: LocalDate,
        årsak: AvventÅrsak,
    ) {
        log.info("Oppretter jobb for å sende slett avvent periode til Utsjekk.")
        FlytJobbRepository(connection).leggTil(
            JobbInput(SendSlettAvventPeriodeUtfører)
                .forSak(sakUtbetalingId)
                .medParameter("tilkjentYtelseId", tilkjentYtelseId.toString())
                .medParameter("saksnummer", saksnummer.toString())
                .medParameter("referanse", referanse.toString())
                .medParameter("personIdent", personIdent)
                .medParameter("fom", fom.toString())
                .medParameter("tom", tom.toString())
                .medParameter("overføres", overføres.toString())
                .medParameter("årsak", årsak.toString())
        )

    }

    fun sendUtbetalingsmelding(
        tilkjentYtelseId: Long,
        behandlingsreferanse: UUID,
        sakUtbetalingId: Long,
        utbetalingsmeldingJson: String,
        utsettUtbetalingEtterSlettAvventPeriode: Boolean,
    ) {
        log.info("Oppretter jobb for å sende utbetalingsmelding til Utsjekk.")
        FlytJobbRepository(connection).leggTil(
            JobbInput(SendUtbetalingsmeldingUtfører)
                .forSak(sakUtbetalingId)
                .medParameter("tilkjentYtelseId", tilkjentYtelseId.toString())
                .medParameter("behandlingsreferanse", behandlingsreferanse.toString())
                .medParameter("utbetalingsmelding", utbetalingsmeldingJson)
                .medNesteKjøring(LocalDateTime.now()
                    .plusSeconds(if (utsettUtbetalingEtterSlettAvventPeriode) utsettUtbetalingAntallSekunder else 0))
        )
    }


}