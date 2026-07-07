package no.nav.aap.utbetal.migrering

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.tilgang.AuthorizationRouteConfig
import no.nav.aap.tilgang.authorizedPost
import no.nav.aap.utbetal.httpCallCounter
import no.nav.aap.utbetal.klienter.helved.UtbetalingRestKlient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

private val log: Logger = LoggerFactory.getLogger("POST /migrering")

fun NormalOpenAPIRoute.migrering(dataSource: DataSource, prometheus: PrometheusMeterRegistry, authConfig: AuthorizationRouteConfig) =

    route("/migrering").authorizedPost<Unit, MigreringsresultatDto, MigreringDto>(authConfig, null) { _, dto ->
        if (Miljø.erProd()) {
            throw IllegalStateException("Migrering er ikke klart for produksjon")
        }
        prometheus.httpCallCounter("/migrering").increment()
        log.info("Migrering kalt med maxAntall: {}, dryRun: {}", dto.maxAntall, dto.dryRun)
        val migreringsresultat = UtførMigreringService(dataSource, UtbetalingRestKlient).utførMigrering(dto.maxAntall, dto.dryRun)
        log.info("Migrering fullført. Migrerte saker: {}, Feilede migreringer: {}", migreringsresultat.migrerteSaker, migreringsresultat.feiledeMigreringer)
        respond(
            response = MigreringsresultatDto(
                migrerteSaker = migreringsresultat.migrerteSaker .map { it.toString() },
                feiledeMigreringer = migreringsresultat.feiledeMigreringer . map { it.toString() }
            )
        )
    }


fun NormalOpenAPIRoute.migrerSak(dataSource: DataSource, prometheus: PrometheusMeterRegistry, authConfig: AuthorizationRouteConfig) =

    route("/migrering/sak").authorizedPost<Unit, MigreringsresultatDto, MigrerSakDto>(authConfig, null) { _, dto ->
        if (Miljø.erProd()) {
            throw IllegalStateException("Migrering er ikke klart for produksjon")
        }
        prometheus.httpCallCounter("/migrering/sak").increment()
        log.info("Migrering kalt for saksnummer: ${dto.saksnummer}, dryRun: ${dto.dryRun}")
        dataSource.transaction { connection ->
            UtførMigreringService(dataSource, UtbetalingRestKlient).utførMigrering(connection, Saksnummer(dto.saksnummer), dto.dryRun)
        }
        log.info("Migrering fullført for saksnummer: ${dto.saksnummer}")
    }