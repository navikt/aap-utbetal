package no.nav.aap.utbetal.utbetaling

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.HttpStatusCode
import javax.sql.DataSource


fun NormalOpenAPIRoute.opprettUtbetalingsjobber(dataSource: DataSource) =

    route("/opprett-utbetalingsjobber").post<Unit, Unit, Unit> {_, _ ->
        UtbetalingService(dataSource).opprettUtbetalingsjobber()
        respondWithStatus(HttpStatusCode.OK)
    }
