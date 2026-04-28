package no.nav.aap.utbetal.klienter.helved

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.utbetal.helved.UtbetalingMelding import org.slf4j.LoggerFactory
import java.net.URI

class UtbetalingV2Klient {
    private val log = LoggerFactory.getLogger(javaClass)

    private val url = URI.create(requiredConfigForKey("integrasjon.utsjekk.url"))

    private val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.utsjekk.scope"),
    )

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider
    )

    fun simuleringUtbetaling(utbetalingMelding: UtbetalingMelding): Simulering {
        log.info("Simulering av utbetaling for saksummer ${utbetalingMelding.sakId} og behandling ${utbetalingMelding.behandlingId}")
        val simuleringUrl = url.resolve("api/dryrun/aap")
        val request = PostRequest(body = utbetalingMelding)
        return requireNotNull(client.post(simuleringUrl, request) { body, _ -> DefaultJsonMapper.fromJson(body) })
    }
}