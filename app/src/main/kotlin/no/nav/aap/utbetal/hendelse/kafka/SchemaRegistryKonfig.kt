package no.nav.aap.utbetal.hendelse.kafka

import no.nav.aap.komponenter.config.requiredConfigForKey
import java.util.Properties

data class SchemaRegistryKonfig(
    val url: String = requiredConfigForKey("KAFKA_SCHEMA_REGISTRY"),
    val user: String = requiredConfigForKey("KAFKA_SCHEMA_REGISTRY_USER"),
    val password: String = requiredConfigForKey("KAFKA_SCHEMA_REGISTRY_PASSWORD")
) {
    fun properties() = Properties().apply {
        this["schema.registry.url"] = url
        this["basic.auth.credentials.source"] = "USER_INFO"
        this["basic.auth.user.info"] = "$user:$password"
    }
}