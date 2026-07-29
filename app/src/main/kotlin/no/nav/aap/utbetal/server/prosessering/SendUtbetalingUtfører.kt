package no.nav.aap.utbetal.server.prosessering

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.utbetal.helved.tilUtbetalingMelding
import no.nav.aap.utbetal.hendelse.kafka.KafkaProdusentKonfig
import no.nav.aap.utbetal.hendelse.konsument.Status
import no.nav.aap.utbetal.hendelse.konsument.UtbetalingDetaljer
import no.nav.aap.utbetal.hendelse.konsument.UtbetalingStatusHendelse
import no.nav.aap.utbetal.hendelse.produsent.UtbetalingProdusent
import no.nav.aap.utbetal.simulering.SimuleringService
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelse
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.utbetal.tilkjentytelse.UtbetalingStatusRepository
import no.nav.aap.utbetal.utbetaling.GjeldendeAvventPeriode
import no.nav.aap.utbetal.utbetaling.GjeldendeAvventPeriodeRepository
import no.nav.aap.utbetal.utbetaling.MeldeperiodeUtbetalingMappingRepository
import java.util.*

class SendUtbetalingUtfører(
    private val connection: DBConnection,
    private val utbetalingProdusentFactory: () -> UtbetalingProdusent = { UtbetalingProdusent(KafkaProdusentKonfig()) },
    private val simuleringServiceFactory: (DBConnection) -> SimuleringService = { SimuleringService(it) },
) : JobbUtfører {
    override fun utfør(input: JobbInput) {
        //OBS: sakId er i dette tilfellet sak_utbetaling_id siden vi ikke har sak_id i utbetalings-appen.
        val sakUtbetalingId = input.sakId()
        val behandlingsreferanse = UUID.fromString(input.parameter("behandlingsreferanse"))

        val tilkjentYtelse = TilkjentYtelseRepository(connection).hent(behandlingsreferanse)
            ?: throw IllegalArgumentException("Finner ikke tilkjent ytelse for behandling: $behandlingsreferanse")

        val meldeperiodeUtbetalingMap = MeldeperiodeUtbetalingMappingRepository(connection)
            .oppdatereMeldeperiodeUtbetalingMapping(sakUtbetalingId, tilkjentYtelse, true)

        val utbetalingMelding = tilkjentYtelse.tilUtbetalingMelding(meldeperiodeUtbetalingMap)

        val utbetalingProdusent = utbetalingProdusentFactory()


        // Lagrer utbetaling status SENDT før vi sender utbetalingshendelsen, slik at vi har en status i databasen som
        // indikerer at vi har sendt utbetalingen til utsjekk. Hvis vi skulle fått en feil i det å sende ut meldingen
        // til utsjekk, så vil vi fortsatt ha en status i databasen som indikerer at vi har forsøkt å sende utbetalingen.
        UtbetalingStatusRepository(connection).oppdaterUtbetalingStatus(tilkjentYtelse.id!!, UtbetalingStatusHendelse(
            status = Status.SENDT,
            detaljer = UtbetalingDetaljer(
                ytelse = "AAP",
                // Lagrer tom liste ved status SENDT, siden vi ikke har fått noen respons fra utsjekk enda. Linjene vil
                // bli oppdatert når vi får respons fra utsjekk i form av en utbetaling-status-hendelse (som blir
                // håndtert av UtbetalingStatusKonsument)
                linjer = listOf(),
            )
        ))

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
                val slettAvventUtbetalingMelding = tilkjentYtelse.copy(perioder = listOf(), avvent = avventUtbetalingFeilregistrering).tilUtbetalingMelding(meldeperiodeUtbetalingMap)
                gjeldendeAvventPeriodeRepo.lagre(
                    GjeldendeAvventPeriode(sakUtbetalingId, Periode(tilkjentYtelse.avvent.fom, tilkjentYtelse.avvent.tom))
                )
                utbetalingProdusent.sendUtbetalingHendelse(behandlingsreferanse.toString(), slettAvventUtbetalingMelding)
            }
        }

        //Send utbetaling
        utbetalingProdusent.sendUtbetalingHendelse(behandlingsreferanse.toString(), utbetalingMelding)
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
            return SendUtbetalingUtfører(connection)
        }

        override fun type(): String {
            return "batch.sendUtbetaling"
        }

        override fun navn(): String {
            return "Sender utbetaling"
        }

        override fun beskrivelse(): String {
            return "Sender utbetaling på Kafka grensesnitt til Utsjekk"
        }
    }

}