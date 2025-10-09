package no.nav.aap.utbetal.trekk

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.tilgang.AuthorizationRouteConfig
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.utbetal.httpCallCounter
import javax.sql.DataSource

fun NormalOpenAPIRoute.hentTrekkListe(dataSource: DataSource, prometheus: PrometheusMeterRegistry, authConfig: AuthorizationRouteConfig) =

    route("/trekk/{saksnummer}").authorizedGet<String, TrekkResponsDto>(routeConfig = authConfig) { saksnummer ->
        prometheus.httpCallCounter("/trekk").increment()

        val trekkListe = dataSource.transaction(readOnly = true) { connection ->
            TrekkRepository(connection).hentTrekk(Saksnummer(saksnummer))
        }
        respond(response = trekkListe.tilDto())
    }

private fun List<Trekk>.tilDto() =
    TrekkResponsDto(map {
        TrekkDto(
            saksnummer = it.saksnummer.toString(),
            behandlingsreferanse = it.behandlingsreferanse,
            dato = it.dato,
            beløp = it.beløp,
            posteringer = it.posteringer.tilDto(),
        )
    })

private fun List<TrekkPostering>.tilDto() =
    map {
        TrekkPosteringDto(
            dato = it.dato,
            beløp = it.beløp,
        )
    }

