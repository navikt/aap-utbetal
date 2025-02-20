package no.nav.aap.utbetal.klienter.helved

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.UUID

class UtbetalingKlient {

    private val log = LoggerFactory.getLogger(UtbetalingKlient::class.java)

    private val url = URI.create(requiredConfigForKey("integrasjon.utsjekk.url"))

    private val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.utsjekk.scope"),
    )

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider
    )

    /*
    fun simulering(simuleringRequest: SimuleringRequest) {
        log.info("Simulering for saksummer ${simuleringRequest.sakId} og behandling ${simuleringRequest.behandlingId}")
        val simuleringUrl = url.resolve("api/simulering/v2")
        val request = PostRequest(body = simuleringRequest)

        client.post<SimuleringRequest, Unit>(simuleringUrl, request)
    }
    */

    fun iverksett(utbetalingRef: UUID, utbetaling: Utbetaling) {
        log.info("Iverksett utbetaling for saksnummer ${utbetaling.sakId}, behandingId ${utbetaling.behandlingId} (${utbetaling.behandlingId.base64ToUUID()}) og utbetalingRef $utbetalingRef")
        val iverksettUrl = url.resolve(("utbetalinger/$utbetalingRef"))
        val request = PostRequest(body = utbetaling)
        client.post<Utbetaling, Unit>(iverksettUrl, request)
    }

}