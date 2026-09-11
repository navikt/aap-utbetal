package no.nav.aap.utbetal.server.prosessering.nytt_grensesnitt

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.utbetal.helved.tilUtbetalingsmelding
import no.nav.aap.utbetal.hendelse.konsument.Status
import no.nav.aap.utbetal.hendelse.konsument.UtbetalingDetaljer
import no.nav.aap.utbetal.hendelse.konsument.UtbetalingStatusHendelse
import no.nav.aap.utbetal.simulering.SimuleringService
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelse
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.utbetal.tilkjentytelse.UtbetalingStatusRepository
import no.nav.aap.utbetal.utbetaling.GjeldendeAvventPeriode
import no.nav.aap.utbetal.utbetaling.GjeldendeAvventPeriodeRepository
import no.nav.aap.utbetal.utbetaling.MeldeperiodeUtbetalingMappingRepository
import no.nav.aap.utbetal.utbetaling.UtbetalingJobbService
import no.nav.aap.utbetal.utbetaling.Utbetalingsmelding
import no.nav.aap.utbetal.utbetaling.UtbetalingsmeldingRepository
import no.nav.aap.utbetal.utbetaling.UtbetalingsmeldingType
import java.util.UUID

class OpprettUtbetalingsmeldingUtfører(
    private val connection: DBConnection,
    private val simuleringServiceFactory: (DBConnection) -> SimuleringService = { SimuleringService(it) },
    ): JobbUtfører {

    override fun utfør(input: JobbInput) {
        //OBS: sakId er i dette tilfellet sak_utbetaling_id siden vi ikke har sak_id i utbetalings-appen.
        val sakUtbetalingId = input.sakId()
        val behandlingsreferanse = UUID.fromString(input.parameter("behandlingsreferanse"))

        val tilkjentYtelse = TilkjentYtelseRepository(connection).hent(behandlingsreferanse)
            ?: throw IllegalArgumentException("Finner ikke tilkjent ytelse for behandling: $behandlingsreferanse")

        val meldeperiodeUtbetalingMap = MeldeperiodeUtbetalingMappingRepository(connection)
            .oppdatereMeldeperiodeUtbetalingMapping(sakUtbetalingId, tilkjentYtelse, true)

        val utbetalingsmelding = tilkjentYtelse.tilUtbetalingsmelding(meldeperiodeUtbetalingMap)
        val utbetalingsmeldingJson = DefaultJsonMapper.toJson(utbetalingsmelding)


        // Lagrer utbetalingsmelding i databasen, slik at vi har en kopi av meldingen som ble sendt til utsjekk.
        // Dette er nyttig for å kunne feilsøke og spore meldinger som er sendt.
        UtbetalingsmeldingRepository(connection).lagre(Utbetalingsmelding(
            sakUtbetalingId = sakUtbetalingId,
            tilkjentYtelseId = tilkjentYtelse.id!!,
            referanse = tilkjentYtelse.behandlingsreferanse,
            utbetalingsmeldingType = UtbetalingsmeldingType.UTBETALING,
            melding = utbetalingsmeldingJson,
        ))

        // Oppdaterer utbetalingsstatus til IKKE_SENDT, siden vi ikke har sendt meldingen til utsjekk enda.
        // Dette er nyttig for å kunne spore status på utbetalinger.
        UtbetalingStatusRepository(connection).oppdaterUtbetalingsstatusV2(
            tilkjentYtelseId = tilkjentYtelse.id,
            referanse = tilkjentYtelse.behandlingsreferanse,
            utbetalingStatusHendelse = UtbetalingStatusHendelse(
                status = Status.IKKE_SENDT,
                detaljer = UtbetalingDetaljer(
                    ytelse = "AAP",
                    // Lagrer tom liste ved status IKKE_SENDT, siden vi ikke har fått noen respons fra utsjekk enda. Linjene vil
                    // bli oppdatert når vi får respons fra utsjekk i form av en utbetaling-status-hendelse (som blir
                    // håndtert av UtbetalingStatusKonsument)
                    linjer = listOf(),
                )
            )
        )

        val utbetalingJobbService = UtbetalingJobbService(connection)

        // Håndtere endring av avvent utbetaling periode
        if (erEndringAvventUtbetaling(sakUtbetalingId, tilkjentYtelse)) {
            val gjeldendeAvventPeriodeRepo = GjeldendeAvventPeriodeRepository(connection)
            val gjeldendeAvventPeriode = gjeldendeAvventPeriodeRepo.hentGjeldendeAvventPeriode(sakUtbetalingId)
            if (gjeldendeAvventPeriode != null && tilkjentYtelse.avvent != null) {
                val nyAvventUtbetaling = tilkjentYtelse.avvent
                val avventUtbetalingFeilregistrering = nyAvventUtbetaling.copy(
                    fom = gjeldendeAvventPeriode.periode.fom,
                    tom = gjeldendeAvventPeriode.periode.tom,
                    feilregistrering = true,
                )

                gjeldendeAvventPeriodeRepo.lagre(
                    GjeldendeAvventPeriode(sakUtbetalingId, Periode(tilkjentYtelse.avvent.fom, tilkjentYtelse.avvent.tom))
                )

                utbetalingJobbService.sendSlettAvventPeriode(
                    tilkjentYtelseId = tilkjentYtelse.id,
                    sakUtbetalingId = sakUtbetalingId,
                    saksnummer = tilkjentYtelse.saksnummer,
                    personIdent = tilkjentYtelse.personIdent,
                    fom = avventUtbetalingFeilregistrering.fom,
                    tom = avventUtbetalingFeilregistrering.tom,
                    overføres = avventUtbetalingFeilregistrering.overføres!!,
                    årsak = avventUtbetalingFeilregistrering.årsak!!
                )
            }
        }

        utbetalingJobbService.sendUtbetalingsmelding(
            tilkjentYtelseId = tilkjentYtelse.id,
            sakUtbetalingId = sakUtbetalingId,
            utbetalingsmeldingJson = utbetalingsmeldingJson
        )
    }

    private fun erEndringAvventUtbetaling(sakUtbetalingId: Long, tilkjentYtelse: TilkjentYtelse): Boolean {
        val gjeldendeAvventPeriode = GjeldendeAvventPeriodeRepository(connection).hentGjeldendeAvventPeriode(sakUtbetalingId)
        val nyAvventPeriode = tilkjentYtelse.avventPeriode()
        if (gjeldendeAvventPeriode != null && nyAvventPeriode != null) {
            if (gjeldendeAvventPeriode.periode != nyAvventPeriode) {
                // Sjekk om det er endring i beløp. Hvis ikke er det ikke mulig å sende feilregistrering av avvent periode.
                val simeringsresultat = simuleringServiceFactory(connection).simuler(tilkjentYtelse)
                return simeringsresultat.perioder.any { periode ->
                    periode.utbetalinger.any { utbetaling ->
                        utbetaling.tidligereUtbetalt != utbetaling.nyttBeløp
                    }
                }
            }
        }
        return false
    }

    private fun TilkjentYtelse.avventPeriode(): Periode? {
        if (avvent != null) {
            return Periode(avvent.fom, avvent.tom)
        }
        return null
    }

    companion object: Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return OpprettUtbetalingsmeldingUtfører(connection)
        }

        override fun type(): String {
            return "batch.opprettUtbetalingsmelding"
        }

        override fun navn(): String {
            return "Opprett utbetalingsmelding"
        }

        override fun beskrivelse(): String {
            return "Opprett utbetalingsmelding"
        }

    }

}