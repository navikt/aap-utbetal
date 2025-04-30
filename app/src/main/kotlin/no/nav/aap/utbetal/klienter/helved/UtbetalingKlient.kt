package no.nav.aap.utbetal.klienter.helved

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.get
import no.nav.aap.komponenter.httpklient.httpclient.request.DeleteMedBodyRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PutRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.utbetaling.helved.base64ToUUID
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.*


enum class UtbetalingStatus {
    SENDT_TIL_OPPDRAG,
    FEILET_MOT_OPPDRAG,
    OK,
    IKKE_PÅBEGYNT,
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

    fun iverksettNy(utbetalingRef: UUID, helvedUtbetaling: Utbetaling) {
        log.info("Iverksett ny utbetaling for saksnummer ${helvedUtbetaling.sakId}, behandingId ${helvedUtbetaling.behandlingId} (${helvedUtbetaling.behandlingId.base64ToUUID()}) og utbetalingRef $utbetalingRef")
        val iverksettUrl = url.resolve("utbetalinger/$utbetalingRef")
        val request = PostRequest(body = helvedUtbetaling)
        client.post<Utbetaling, Unit>(iverksettUrl, request) { _, _ -> }
    }

    fun iverksettEndring(utbetalingRef: UUID, helvedUtbetaling: Utbetaling) {
        log.info("Iverksett endring utbetaling for saksnummer ${helvedUtbetaling.sakId}, behandingId ${helvedUtbetaling.behandlingId} (${helvedUtbetaling.behandlingId.base64ToUUID()}) og utbetalingRef $utbetalingRef")
        val iverksettUrl = url.resolve(("utbetalinger/$utbetalingRef"))
        val request = PutRequest(body = helvedUtbetaling)
        client.put<Utbetaling, Unit>(iverksettUrl, request) { _, _ -> }
    }

    fun opphør(utbetalingRef: UUID, helvedUtbetaling: Utbetaling) {
        log.info("Opphør av utbetaling for saksnummer ${helvedUtbetaling.sakId}, behandingId ${helvedUtbetaling.behandlingId} (${helvedUtbetaling.behandlingId.base64ToUUID()}) og utbetalingRef $utbetalingRef")
        val opphørUrl = url.resolve(("utbetalinger/$utbetalingRef"))
        val request = DeleteMedBodyRequest<Utbetaling>(body = helvedUtbetaling)
        client.deleteMedBody<Utbetaling, Unit>(opphørUrl, request) { _, _ -> }
    }

    fun hentUtbetaling(utbetalingRef: UUID): Utbetaling {
        log.info("Hent utbetaling for utbetalingRef $utbetalingRef")
        val hentUtbetalingUrl = url.resolve(("utbetalinger/$utbetalingRef"))
        return client.get<Utbetaling>(hentUtbetalingUrl, GetRequest())!!
    }

    fun hentStatus(utbetalingRef: UUID): UtbetalingStatus {
        val hentStatusUrl = url.resolve(("utbetalinger/$utbetalingRef/status"))
        val request = GetRequest()
        return client.get<UtbetalingStatus>(hentStatusUrl, request)!!
    }

    fun simuleringUtbetaling(utbetalingRef: UUID, utbetaling: Utbetaling): Simulering {
        log.info("Simulering av utbetaling for saksummer ${utbetaling.sakId} og behandling ${utbetaling.behandlingId}")
        val simuleringUrl = url.resolve("utbetalinger/$utbetalingRef/simuler")
        val request = PostRequest(body = utbetaling)
        return requireNotNull(client.post(simuleringUrl, request) { body, _ -> DefaultJsonMapper.fromJson(body) })
    }

    fun simuleringOpphør(utbetalingRef: UUID, utbetaling: Utbetaling): Simulering {
        log.info("Simulering av opphør for saksummer ${utbetaling.sakId} og behandling ${utbetaling.behandlingId}")
        val simuleringUrl = url.resolve("utbetalinger/$utbetalingRef/simuler")
        val request = DeleteMedBodyRequest(body = utbetaling)
        return requireNotNull(client.deleteMedBody(simuleringUrl, request) { body, _ -> DefaultJsonMapper.fromJson(body) })
    }

}