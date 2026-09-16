package no.nav.aap.utbetal.hendelse.produsent

import no.nav.aap.utbetal.hendelse.kafka.KafkaProdusent
import no.nav.aap.utbetal.hendelse.kafka.KafkaProdusentKonfig

const val AAP_UTBETALING_HENDELSE_TOPIC = "aap.utbetaling.v1"

class UtbetalingProdusent(
    config: KafkaProdusentKonfig<String, String>,
): KafkaProdusent<String, String>(
    topic = AAP_UTBETALING_HENDELSE_TOPIC,
    config = config,
    producerName = "AapUtbetalingProdusent",
), UtbetalingsmeldingSender