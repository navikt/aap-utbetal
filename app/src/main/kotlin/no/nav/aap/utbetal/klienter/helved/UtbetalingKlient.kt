package no.nav.aap.utbetal.klienter.helved

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.utbetal.klienter.helved.api.SimuleringRequest
import no.nav.utsjekk.kontrakter.iverksett.IverksettV2Dto
import org.slf4j.LoggerFactory
import java.net.URI

class UtbetalingKlient {

    private val log = LoggerFactory.getLogger(UtbetalingKlient::class.java)

    private val url = URI.create(requiredConfigForKey("integrasjon.helved-utbetaling.url"))

    private val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.helved-utbetaling.scope"),
    )

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider
    )

    fun simulering(simuleringRequest: SimuleringRequest) {
        log.info("Simulering for saksummer ${simuleringRequest.sakId} og behandling ${simuleringRequest.behandlingId}")
        val simuleringUrl = url.resolve("api/simulering/v2")
        val request = PostRequest(body = simuleringRequest)

        client.post<SimuleringRequest, Unit>(simuleringUrl, request)
    }

    fun iverksett(iverksettDto: IverksettV2Dto) {
        log.info("Iverksett utbetaling for saksummer ${iverksettDto.sakId} og behandling ${iverksettDto.behandlingId}")
        val iverksettUrl = url.resolve(("api/iverksetting/v2"))
        val request = PostRequest(body = iverksettDto)
        client.post<IverksettV2Dto, Unit>(iverksettUrl, request)
    }

}