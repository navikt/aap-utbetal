package no.nav.aap.utbetal.hendelse.kafka

import no.nav.aap.komponenter.config.requiredConfigForKey
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.StringDeserializer
import java.util.Properties

data class KafkaKonsumentKonfig<K, V>(
    val applicationId: String = requiredConfigForKey("NAIS_APP_NAME"),
    val maxPollRecords: Int = 1,
    val autoOffsetReset: String = "earliest",
    val enableAutoCommit: Boolean = false,
    val brokers: String = requiredConfigForKey("KAFKA_BROKERS"),
    val ssl: SslKonfig? = SslKonfig(),
    val schemaRegistry: SchemaRegistryKonfig? = SchemaRegistryKonfig(),
    val compressionType: String = "snappy",
    val additionalProperties: Properties = Properties(),
    val keyDeserializer: Class<out Deserializer<*>> = StringDeserializer::class.java,
    val valueDeserializer: Class<out Deserializer<*>> = StringDeserializer::class.java
) {
    fun consumerProperties(consumerName: String): Properties = Properties().apply {
        this[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG] = brokers
        this[CommonClientConfigs.CLIENT_ID_CONFIG] = "$applicationId-$consumerName"

        ssl?.let { putAll(it.properties()) }
        schemaRegistry?.let { putAll(it.properties()) }
        putAll(additionalProperties)

        this[ConsumerConfig.GROUP_ID_CONFIG] = applicationId
        this[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = maxPollRecords
        this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = autoOffsetReset
        this[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = enableAutoCommit
        this[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = keyDeserializer
        this[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = valueDeserializer
        put("specific.avro.reader", true)
    }
}

