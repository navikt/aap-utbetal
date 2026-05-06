package no.nav.aap.utbetal.hendelse.konsument

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.utbetal.hendelse.kafka.KafkaKonsument
import no.nav.aap.utbetal.hendelse.kafka.KafkaKonsumentKonfig
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.utbetal.tilkjentytelse.UtbetalingStatusRepository
import no.nav.aap.utbetal.utbetaling.SakUtbetalingRepository
import no.nav.aap.utbetaling.helved.base64ToUUID
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.LoggerFactory
import java.util.*
import javax.sql.DataSource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

const val UTBETALING_STATUS_TOPIC = "helved.status.v1"

const val FAGSYSTEM_KEY = "fagsystem"
const val FAGSYSTEM_AAP_VALUE = "AAP"

class UtbetalingStatusKonsument(
    config: KafkaKonsumentKonfig<String, String>,
    pollTimeout: Duration = 10.seconds,
    closeTimeout: Duration = 30.seconds,
    topic: String = UTBETALING_STATUS_TOPIC,
    private val dataSource: DataSource,
) : KafkaKonsument<String, String>(
    topic = topic,
    config = config,
    pollTimeout = pollTimeout,
    closeTimeout = closeTimeout,
    consumerName = "AapUtbetalUtbetalingStatusHendelse",
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("team-logs")

    override fun håndter(meldinger: ConsumerRecords<String, String>) {
        meldinger.forEach { håndterEnMelding(it) }
    }

    private fun håndterEnMelding(melding: ConsumerRecord<String, String>) {
            log.info(
            "Behandler utbetaling-status-record med id: {}, partition {}, offset: {}",
            melding.key(),
            melding.partition(),
            melding.offset(),
        )
        val fagsystem = melding.headers().lastHeader(FAGSYSTEM_KEY)
        if (fagsystem == null) {
            log.info("Fagsystem header ikke funnet. Hopper over melding: ${melding.value()}")
            return
        } else if  (fagsystem.value().toString(Charsets.UTF_8) != FAGSYSTEM_AAP_VALUE) {
            log.info("Melding fra utbetaling-status med id: {} er for et annet fagsystem, hopper over", melding.key())
            return
        }
        try {
            dataSource.transaction { connection ->
                val utbetalingStatusHendelse = DefaultJsonMapper.fromJson<UtbetalingStatusHendelse>(melding.value())
                val behandlingRef = UUID.fromString(melding.key())

                val tilkjentYtelse = TilkjentYtelseRepository(connection).hentTilkjentYtelseLight(behandlingRef)
                if (tilkjentYtelse != null) {
                    verifisertUtbetalingslinjer(behandlingRef, utbetalingStatusHendelse)
                    val sakUtbetaling = SakUtbetalingRepository(connection).hent(tilkjentYtelse.saksnummer)
                    if (sakUtbetaling != null) {
                        if (sakUtbetaling.migrertTilKafka != null) {
                            UtbetalingStatusRepository(connection).oppdaterUtbetalingStatus(tilkjentYtelse.id, utbetalingStatusHendelse)
                        } else {
                            throw IllegalStateException("Kunne ikke lagre tilkjent ytelse for utbetaling-status")
                        }
                    }
                } else {
                    log.info("Fant ikke behandling for $behandlingRef. Tolker derfor denne til å være utbetaling_id og tilhører gammel utbetalingsløsning.")
                }
            }
        } catch (exception: Exception) {
            log.error("Kunne ikke håndtere melding fra utbetaling-status: ${melding.key()}", exception)
            secureLogger.error("Kunne ikke håndtere melding fra utbetaling-status: ${melding.key()} med verdi: ${melding.value()}", exception)
            throw exception
        }
    }

    private fun verifisertUtbetalingslinjer(behandlingRef: UUID, utbetalingStatusHendelse: UtbetalingStatusHendelse) {
        if (utbetalingStatusHendelse.detaljer.linjer.isEmpty()) {
            return
        }
        val unikeBehandlingRefs = utbetalingStatusHendelse.detaljer.linjer.map { it.behandlingId.base64ToUUID() }.toSet()
        if (unikeBehandlingRefs.size > 1) {
            throw IllegalStateException("Utbetaling status hendelse for behandling $behandlingRef har linjer med flere ulike behandling referanser: $unikeBehandlingRefs")
        }
        if (unikeBehandlingRefs.first() != behandlingRef) {
            throw IllegalStateException("Utbetaling status hendelse for behandling $behandlingRef har linjer med behandling referanse ${unikeBehandlingRefs.first()} som ikke samsvarer med behandling referansen i hendelsen")
        }
    }

}



