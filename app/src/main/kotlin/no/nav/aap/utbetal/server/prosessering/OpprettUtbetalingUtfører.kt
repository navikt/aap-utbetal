package no.nav.aap.utbetal.server.prosessering

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.utbetal.utbetaling.SakUtbetalingRepository
import no.nav.aap.utbetal.utbetaling.Utbetaling
import no.nav.aap.utbetal.utbetaling.UtbetalingBeregner
import no.nav.aap.utbetal.utbetaling.UtbetalingJobbService
import no.nav.aap.utbetal.utbetaling.UtbetalingRepository
import no.nav.aap.utbetaling.UtbetalingStatus
import java.time.LocalDate
import java.util.UUID

class OpprettUtbetalingUtfører(private val connection: DBConnection): JobbUtfører {
    override fun utfør(input: JobbInput) {
        val saksnummer = input.parameter("saksnummer")
        val behandlingsreferanse = input.parameter("behandlingsreferanse")

        val utbetaling = opprettUtbetaling(
            saksnummer = Saksnummer(saksnummer),
            behandlingsreferanse = UUID.fromString(behandlingsreferanse)
        )

        if (utbetaling.utbetalingStatus != UtbetalingStatus.INGEN_PERIODER) {
            UtbetalingJobbService(connection).overførUtbetalingJobb(utbetaling.id!!)
        }
    }

    private fun opprettUtbetaling(saksnummer: Saksnummer, behandlingsreferanse: UUID): Utbetaling {
        val tilkjentYtelseRepo = TilkjentYtelseRepository(connection)
        val nyTilkjentYtelse = tilkjentYtelseRepo.hent(behandlingsreferanse) ?: throw IllegalArgumentException("Finner ikke tilkjent ytelse for behandling: $behandlingsreferanse")
        val forrigeTilkjentYtelse = nyTilkjentYtelse.forrigeBehandlingsreferanse?.let {tilkjentYtelseRepo.hent(it)}


        val sakUtbetaling = SakUtbetalingRepository(connection).hent(saksnummer) ?: throw IllegalArgumentException("Finner ikke sak")
        val utbetaling = UtbetalingBeregner().tilkjentYtelseTilUtbetaling(sakUtbetaling.id!!, nyTilkjentYtelse, forrigeTilkjentYtelse, LocalDate.now().minusDays(1)) //TODO: Blir i dag minus en dag riktig?

        val utbetalingId = UtbetalingRepository(connection).lagre(utbetaling)
        return utbetaling.copy(id = utbetalingId)
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