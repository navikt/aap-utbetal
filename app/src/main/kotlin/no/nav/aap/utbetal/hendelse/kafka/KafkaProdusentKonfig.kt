package no.nav.aap.utbetal.hendelse.kafka

import no.nav.aap.komponenter.config.requiredConfigForKey
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import java.util.*

data class KafkaProdusentKonfig<K, V>(
    val applicationId: String = requiredConfigForKey("NAIS_APP_NAME"),
    val brokers: String = requiredConfigForKey("KAFKA_BROKERS"),
    val ssl: SslKonfig? = SslKonfig(),
    val schemaRegistry: SchemaRegistryKonfig? = SchemaRegistryKonfig(),
    val additionalProperties: Properties = Properties(),
) {
    fun producerProperties(producerName: String): Properties = Properties().apply {
        this[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG] = brokers
        this[CommonClientConfigs.CLIENT_ID_CONFIG] = "$applicationId-$producerName"
        this[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
        this[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
        this[ProducerConfig.ACKS_CONFIG] = "all"
        this[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG] = true

        ssl?.let { putAll(it.properties()) }
        schemaRegistry?.let { putAll(it.properties()) }
        putAll(additionalProperties)

        put("specific.avro.reader", true)
    }
}
