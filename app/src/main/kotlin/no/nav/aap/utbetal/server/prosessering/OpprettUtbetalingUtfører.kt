package no.nav.aap.utbetal.server.prosessering

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.utbetal.utbetaling.SakUtbetalingRepository
import no.nav.aap.utbetal.utbetaling.UtbetalingJobbService
import no.nav.aap.utbetal.utbetaling.UtbetalingService
import java.time.LocalDateTime
import java.util.*

class OpprettUtbetalingUtfører(private val connection: DBConnection): JobbUtfører {
    override fun utfør(input: JobbInput) {
        val behandlingsreferanse = input.parameter("behandlingsreferanse")

        val utbetalinger = UtbetalingService(connection).opprettUtbetalinger(
            behandlingsreferanse = UUID.fromString(behandlingsreferanse)
        )

        if (utbetalinger.alle().isNotEmpty()) {
            //Skal alltid finne SakUtbetaling her.
            val sakUtbetaling = SakUtbetalingRepository(connection).hent(utbetalinger.alle().first().saksnummer)!!

            val utbetalingJobbService = UtbetalingJobbService(connection)
            val slettAvventUtbetaling = utbetalinger.utbetalingMedSlettingAvAvventPeriode
            if (slettAvventUtbetaling != null) {
                utbetalingJobbService.overførUtbetalingJobb(
                    sakUtbetaling,
                    slettAvventUtbetaling.id!!,
                )
                // Dersom disse utbetalingene fører til en sletting av avvent utbetaling, så må vi vente med å overføre
                // resten disse til økonomi til 10 sekunder etter at slettet er oversendt. Dette gjøres for å hindre
                // at utbetalinger blir behandlet i feil rekkefølge.
                val tidspunktForUtbetalingDersomSlettingAvAvventPeriode = LocalDateTime.now().plusSeconds(10)
                val overførOm10Sekunder = fun (utbetaling: no.nav.aap.utbetal.utbetaling.Utbetaling) {
                    utbetalingJobbService.overførUtbetalingJobb(
                        sakUtbetaling,
                        utbetaling.id!!,
                        tidspunktForUtbetalingDersomSlettingAvAvventPeriode
                    )
                }
                utbetalinger.endringUtbetalinger
                    .forEach { overførOm10Sekunder(it) }
                utbetalinger.nyeUtbetalinger
                    .forEach { overførOm10Sekunder(it) }
            } else {
                utbetalinger.alle().forEach { utbetaling -> utbetalingJobbService.overførUtbetalingJobb(sakUtbetaling, utbetaling.id!!) }
            }
        }
    }

    companion object: Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return OpprettUtbetalingUtfører(connection)
        }

        override fun type(): String {
            return "batch.opprettUtbetaling"
        }

        override fun navn(): String {
            return "Opprett utbetaling"
        }

        override fun beskrivelse(): String {
            return "Opprett utbetaling"
        }

    }

}