package no.nav.aap.utbetal.hendelse.konsument

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.utbetal.hendelse.kafka.KafkaKonsumentKonfig
import no.nav.aap.utbetal.hendelse.kafka.KafkaKonsument
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.LoggerFactory
import javax.sql.DataSource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

const val UTBETALING_STATUS_TOPIC = "helved.status.v1"


class UtbetalingStatusKonsument(
    config: KafkaKonsumentKonfig<String, String>,
    pollTimeout: Duration = 10.seconds,
    closeTimeout: Duration = 30.seconds,
    private val dataSource: DataSource,
) : KafkaKonsument<String, String>(
    topic = UTBETALING_STATUS_TOPIC,
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
        try {
            dataSource.transaction { connection ->
                val utbetalingStatus = DefaultJsonMapper.fromJson<UtbetalingStatus>(melding.value())
                //TODO: Håndter melding
                //UtbetalingStatusService(connection).håndterUtbetalingStatus(melding.value())

            }
        } catch (exception: Exception) {
            log.error("Kunne ikke håndtere melding fra utbetaling-status: ${melding.key()}", exception)
            secureLogger.error("Kunne ikke håndtere melding fra utbetaling-status: ${melding.key()} med verdi: ${melding.value()}", exception)
            throw exception
        }
    }


}



