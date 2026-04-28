package no.nav.aap.utbetal.hendelse.konsument

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.utbetal.felles.YtelseDetaljer
import no.nav.aap.utbetal.hendelse.kafka.KafkaKonsumentKonfig
import no.nav.aap.utbetal.hendelse.kafka.SchemaRegistryKonfig
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelse
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.utbetal.tilkjentytelse.UtbetalingStatusRepository
import no.nav.aap.utbetal.utbetaling.SakUtbetalingRepository
import no.nav.aap.utbetaling.helved.toBase64
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.kafka.common.serialization.StringSerializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.milliseconds

class UtbetalingStatusKonsumentTest {

    @Test
    fun `Motta en hendelse i konsument`() {
        val periode = Periode(
            fom = LocalDate.parse("2026-03-02"),
            tom = LocalDate.parse("2026-03-15"),
        )
        val behandlingRef = UUID.randomUUID()
        lagreTilkjentYtelse(behandlingRef, periode)
        lagreSakUtbetaling(behandlingRef)

        val producerProps = Properties().apply {
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.bootstrapServers)
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
        }

        KafkaProducer<String, String>(producerProps).use { produsent ->
            val hendelse = lagUtbetalingStatusHendelse(behandlingRef, periode)
            val value = DefaultJsonMapper.toJson(hendelse)
            produsent.send(
                ProducerRecord(LOKAL_UTBETALING_STATUS_TOPIC, null,behandlingRef.toString(), value, listOf(RecordHeader("fagsystem", "AAP".toByteArray())))
            )
            produsent.flush()
        }


        val pollThread = thread(start = true) {
            konsument.konsumer()
        }

        while (konsument.antallMeldinger == 0) {
            Thread.sleep(100)
        }

        assertThat(konsument.antallMeldinger).isEqualTo(1)

        konsument.lukk()
        kafka.stop()
        pollThread.join()

        dataSource.transaction { connection ->
            val utbetalingStatus = UtbetalingStatusRepository(connection).hent(behandlingRef)
            assertThat(utbetalingStatus).isNotNull()
            assertThat(utbetalingStatus?.status).isEqualTo(Status.OK)
        }
    }

    private fun lagreSakUtbetaling(behandlingRef: UUID) {
        dataSource.transaction { connection ->
            val saksnummer = TilkjentYtelseRepository(connection).hent(behandlingRef)!!.saksnummer
            SakUtbetalingRepository(connection).lagre(saksnummer, true)
        }
    }

    private fun lagreTilkjentYtelse(behandlingRef: UUID, periode: Periode) {
        dataSource.transaction { connection ->
            lagTilkjentYtelse(behandlingRef, periode).let { tilkjentYtelse ->
                TilkjentYtelseRepository(connection).lagreTilkjentYtelse(tilkjentYtelse)
            }
        }
    }

    private fun lagTilkjentYtelse(behandlingRef: UUID, periode: Periode): TilkjentYtelse {
        return TilkjentYtelse(
            id = 123L,
            saksnummer = Saksnummer("sak123"),
            behandlingsreferanse = behandlingRef,
            forrigeBehandlingsreferanse = UUID.randomUUID(),
            personIdent = "01017012345",
            vedtakstidspunkt = LocalDateTime.now(),
            beslutterId = "saksbehandler2",
            saksbehandlerId = "saksbehandler1",
            perioder = listOf(
                TilkjentYtelsePeriode(
                    periode = periode,
                    detaljer = YtelseDetaljer(
                        redusertDagsats = Beløp(1000),
                        gradering = Prosent(100),
                        dagsats = Beløp(1000),
                        grunnlag = Beløp(1000),
                        grunnlagsfaktor = GUnit(6),
                        grunnbeløp = Beløp(1000),
                        antallBarn = 0,
                        barnetilleggsats = Beløp(0),
                        barnetillegg = Beløp(0),
                        utbetalingsdato = LocalDate.now(),
                        meldeperiode = periode,
                        barnepensjonDagsats = Beløp(0)
                    )
                )
            ),
            nyMeldeperiode = periode,
        )
    }

    private fun lagUtbetalingStatusHendelse(behandlingRef: UUID, periode: Periode) =
        UtbetalingStatusHendelse(
            status = Status.OK,
            detaljer = UtbetalingDetaljer(
                ytelse = "AAP",
                linjer = listOf(
                    UtbetalingLinje(
                        behandlingId = behandlingRef.toBase64(),
                        periode = periode,
                        vedtakssats = 1000u,
                        beløp = 1000u,
                        klassekode = "ABC",
                    )
                )
            ),
            error = null
        )

    val konsument = UtbetalingStatusKonsument(
        config = testConfig(kafka.bootstrapServers),
        dataSource = dataSource,
        pollTimeout = 50.milliseconds,
        topic = LOKAL_UTBETALING_STATUS_TOPIC,
    )

    companion object {
        private const val LOKAL_UTBETALING_STATUS_TOPIC = "lokal.helved.status.v1"
        private val logger = LoggerFactory.getLogger(UtbetalingStatusKonsument::class.java)
        val kafka: KafkaContainer = KafkaContainer(DockerImageName.parse("apache/kafka-native:4.1.0"))
            .withReuse(true)
            .waitingFor(Wait.forListeningPort())
            .withStartupTimeout(Duration.ofSeconds(60))
            .withLogConsumer { Slf4jLogConsumer(logger) }

        init {
            System.setProperty("INTEGRASJON_UFORE_VEDTAK_TOPIC", LOKAL_UTBETALING_STATUS_TOPIC)
            print("")
        }

        private lateinit var dataSource: TestDataSource

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            dataSource = TestDataSource()
            kafka.start()
        }

        @AfterAll
        @JvmStatic
        internal fun afterAll() {
            kafka.stop()
            dataSource.close()
        }
    }

}

private fun testConfig(brokers: String) = KafkaKonsumentKonfig<String, String>(
    applicationId = "behandlingsflyt-test",
    brokers = brokers,
    ssl = null,
    schemaRegistry = SchemaRegistryKonfig(
        url = "mock://schema-registry",
        user = "",
        password = "",
    )
)
