package no.nav.aap.utbetal.server.prosessering.nytt_grensesnitt

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.utbetal.hendelse.konsument.Status
import no.nav.aap.utbetal.hendelse.konsument.UtbetalingDetaljer
import no.nav.aap.utbetal.hendelse.konsument.UtbetalingStatusHendelse
import no.nav.aap.utbetal.klienter.helved.Avvent
import no.nav.aap.utbetal.klienter.helved.SlettAvventUtbetalingMelding
import no.nav.aap.utbetal.klienter.helved.UtbetalingV2Klient
import no.nav.aap.utbetal.kodeverk.AvventÅrsak
import no.nav.aap.utbetal.tilkjentytelse.UtbetalingStatusRepository
import java.time.LocalDate
import java.util.UUID

class SendSlettAvventPeriodeUtfører(private val connection: DBConnection): JobbUtfører {

    override fun utfør(input: JobbInput) {
        val tilkjentYtelseId = input.parameter("tilkjentYtelseId").toLong()
        val saksnummer = input.parameter("saksnummer")
        val personIdent = input.parameter("personIdent")
        val fom = LocalDate.parse(input.parameter("fom"))
        val tom = LocalDate.parse(input.parameter("tom"))
        val overføres = LocalDate.parse(input.parameter("overføres"))
        val årsak = AvventÅrsak.valueOf(input.parameter("årsak"))


        //Opprettet en tilfeldig referanse for sletting av avvent periode.
        val referanse = UUID.randomUUID()

        UtbetalingStatusRepository(connection).oppdaterUtbetalingsstatus(
            tilkjentYtelseId = tilkjentYtelseId,
            referanse = referanse,
            utbetalingStatusHendelse = UtbetalingStatusHendelse(
                status = Status.SENDT,
                detaljer = UtbetalingDetaljer(
                    ytelse = "AAP",
                    linjer = listOf(),
                )
            )
        )

        UtbetalingV2Klient().slettAvventPeriode(UUID.randomUUID(),
            SlettAvventUtbetalingMelding(
                sakId = saksnummer,
                personident = personIdent,
                avvent = Avvent(
                    fom = fom,
                    tom = tom,
                    overføres = overføres,
                    årsak = årsak,
                    feilregistrering = true
                )
            )
        )
    }

    companion object: Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return SendSlettAvventPeriodeUtfører(connection)
        }

        override fun type(): String {
            return "batch.sendSlettAvventPeriode"
        }

        override fun navn(): String {
            return "Send slett avvent periode"
        }

        override fun beskrivelse(): String {
            return "Sender slett avvent periode til utsjekk"
        }

    }


}
