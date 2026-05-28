package no.nav.aap.utbetal.migrering

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.utbetal.hendelse.konsument.Status
import no.nav.aap.utbetal.hendelse.konsument.UtbetalingStatusHendelse
import no.nav.aap.utbetal.klienter.helved.Migrering
import no.nav.aap.utbetal.klienter.helved.MigreringRequest
import no.nav.aap.utbetal.klienter.helved.UtbetalingKlient
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.utbetal.tilkjentytelse.UtbetalingStatusRepository
import no.nav.aap.utbetal.utbetaling.MeldeperiodeUtbetalingMappingRepository
import no.nav.aap.utbetal.utbetaling.SakUtbetaling
import no.nav.aap.utbetal.utbetaling.SakUtbetalingRepository
import no.nav.aap.utbetal.utbetaling.UtbetalingData
import no.nav.aap.utbetal.utbetaling.UtbetalingRepository
import no.nav.aap.utbetal.utbetaling.UtbetalingService
import no.nav.aap.utbetaling.UtbetalingStatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*
import javax.sql.DataSource
import kotlin.time.measureTimedValue


data class Migreringsresultat(
    val migrerteSaker: List<Saksnummer>,
    val feiledeMigreringer: List<Saksnummer>
)

class UtførMigreringService(private val dataSource: DataSource) {

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    fun utførMigrering(maxAntall: Int, dryRun: Boolean = true): Migreringsresultat {
        val (resultat, duration) = measureTimedValue {
            val sakerTilMigrering = dataSource.transaction(readOnly = true) { connection ->
                SakUtbetalingRepository(connection).hentSakerForMigrering(maxAntall)
            }
            log.info("Fant ${sakerTilMigrering.size} saker til migrering")

            val migrerteSaker = mutableListOf<Saksnummer>()
            val feiledeMigreringer = mutableListOf<Saksnummer>()
            sakerTilMigrering.forEach { sakUtbetaling ->
                try {
                    dataSource.transaction { connection ->
                        utførMigrering(connection, sakUtbetaling.saksnummer, dryRun)
                    }
                    migrerteSaker.add(sakUtbetaling.saksnummer)
                    log.info("Migrering av sak ${sakUtbetaling.saksnummer} fullført")
                } catch (e: Exception) {
                    log.error("Feil ved migrering av sak ${sakUtbetaling.saksnummer}: ${e.message}", e)
                }
            }
            Migreringsresultat(migrerteSaker,feiledeMigreringer)
        }
        log.info("Migrering av ${resultat.migrerteSaker.size} saker fullført på ${duration.inWholeSeconds} sekunder. Feilede migreringer: ${resultat.feiledeMigreringer.size}")
        return resultat
    }

    private fun utførMigrering(connection: DBConnection, saksnummer: Saksnummer, dryRun: Boolean) {

        val sakUtbetaling = SakUtbetalingRepository(connection).hent(saksnummer)

        // Ikke gjør noe dersom saken allerede er migrert
        if (sakUtbetaling != null && sakUtbetaling.migrertTilKafka != null) {
            return
        }

        // Opprett UtbetalingStatus for alle tilkjent ytelse. Status hentes fra utbetalinger.
        opprettUtbetalingStatus(connection, saksnummer, dryRun)

        // Opprett mapping for alle utbetalinger knyttet til denne saken. Hentes ut med utbetaling-tidslinje.")
        val utbetalingTidslinje = UtbetalingService(connection).lagUtbetalingTidslinje(saksnummer)
        val uidTilPeriodeMap = opprettUtbetalingMapping(connection, sakUtbetaling!!, utbetalingTidslinje, dryRun)


        val migreringRequest = MigreringRequest(uidTilPeriodeMap.keys.toSet().map { Migrering(it, it) })
        if (!dryRun) {
            UtbetalingKlient.migrering(migreringRequest)
        }
    }

    private fun opprettUtbetalingStatus(connection: DBConnection, saksnummer: Saksnummer, dryRun: Boolean) {
        val tilkjentYtelseListe = TilkjentYtelseRepository(connection).hentAlleTilkjentYtelseForSaksnummer(saksnummer)

        val utbetalingerForSak = UtbetalingRepository(connection).hent(saksnummer)
        val alleUtbetalingerErBekreftet = utbetalingerForSak.all { it.utbetalingStatus == UtbetalingStatus.BEKREFTET }

        val utbetalingStatusRepository = UtbetalingStatusRepository(connection)
        tilkjentYtelseListe.forEach { tilkjentYtelse ->
            if (alleUtbetalingerErBekreftet) {
                // Finn tidspunkt for bekreftelse av utbetaling. Henter det fra siste utbetaling for saken, og dersom det ikke finnes, sett til nå.
                val utbetalingBekreftetTidspunkt = utbetalingerForSak.map { it.utbetalingEndret ?: it.utbetalingOversendt } .lastOrNull() ?: LocalDateTime.now()

                if (!dryRun) {
                    utbetalingStatusRepository.oppdaterUtbetalingStatus(
                        tilkjentYtelseId = tilkjentYtelse.id!!,
                        utbetalingStatusHendelse = UtbetalingStatusHendelse(Status.OK),
                        statusEndringTidspunkt = utbetalingBekreftetTidspunkt,
                        migrertFraGammeltApi = true
                    )
                }
            } else {
                throw IllegalStateException("Ikke alle utbetalinger for sak $saksnummer er bekreftet. Kan ikke opprette utbetaling status for tilkjent ytelse ${tilkjentYtelse.id}")
            }

        }
    }

    private fun opprettUtbetalingMapping(
        connection: DBConnection,
        sakUtbetaling: SakUtbetaling,
        utbetalingTidslinje: Tidslinje<UtbetalingData>,
        dryRun: Boolean
    ): Map<UUID, Periode> {

        val uidTilPerioderMap = mutableMapOf<UUID, MutableList<Periode>>()
        utbetalingTidslinje.segmenter().forEach { s ->
            val periodeList = uidTilPerioderMap[s.verdi.utbetalingRef]
            if (periodeList == null) {
                uidTilPerioderMap[s.verdi.utbetalingRef] = mutableListOf(s.periode)
            } else {
                uidTilPerioderMap[s.verdi.utbetalingRef] = (periodeList + s.periode).toMutableList()
            }
        }

        val uidTilPeriodeMap = uidTilPerioderMap.map { it.key to Periode(it.value.min().fom, it.value.max().tom) }.toMap()
        val meldeperiodeUtbetalingMappingRepository = MeldeperiodeUtbetalingMappingRepository(connection)
        sjekkOmPerioderOverlapper(sakUtbetaling.saksnummer, uidTilPeriodeMap.values.toList())

        if (!dryRun) {
            uidTilPeriodeMap.forEach { (uid, periode) ->
                meldeperiodeUtbetalingMappingRepository.lagreMeldeperiodeUtbetalingMapping(sakUtbetaling.id!!, periode, uid)
            }
        }
        return uidTilPeriodeMap
    }


    private fun sjekkOmPerioderOverlapper(saksnummer: Saksnummer, perioder: List<Periode>) {
        val sortert = perioder.sortedBy { it.fom }
        for (i in 0 until sortert.size - 1) {
            if (sortert[i].tom >= sortert[i + 1].fom) {
                throw IllegalStateException("Perioder overlapper for $saksnummer: ${sortert[i]} og ${sortert[i + 1]}")
            }
        }
    }

}