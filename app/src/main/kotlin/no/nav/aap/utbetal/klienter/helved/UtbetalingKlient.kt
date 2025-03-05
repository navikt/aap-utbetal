package no.nav.aap.utbetal.klienter.helved

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.get
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.UUID

data class OppdragStatusDto(
    val status: OppdragStatus,
    val feilmelding: String? = null
)

enum class OppdragStatus {
    LAGT_PÅ_KØ,
    KVITTERT_OK,
    KVITTERT_MED_MANGLER,
    KVITTERT_FUNKSJONELL_FEIL,
    KVITTERT_TEKNISK_FEIL,
    KVITTERT_UKJENT,
    OK_UTEN_UTBETALING,
}

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

    fun iverksett(utbetalingRef: UUID, helvedUtbetaling: Utbetaling) {
        log.info("Iverksett utbetaling for saksnummer ${helvedUtbetaling.sakId}, behandingId ${helvedUtbetaling.behandlingId} (${helvedUtbetaling.behandlingId.base64ToUUID()}) og utbetalingId $utbetalingRef")
        val iverksettUrl = url.resolve(("utbetalinger/$utbetalingRef"))
        val request = PostRequest(body = helvedUtbetaling)
        client.post<Utbetaling, Unit>(iverksettUrl, request) { _, _ -> }
    }

    fun hentStatus(utbetalingRef: UUID): OppdragStatusDto {
        val iverksettUrl = url.resolve(("utbetalinger/$utbetalingRef/status"))
        val request = GetRequest()
        return client.get<OppdragStatusDto>(iverksettUrl, request)!!
    }


}