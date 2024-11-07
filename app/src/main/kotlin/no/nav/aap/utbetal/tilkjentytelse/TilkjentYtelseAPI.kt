package no.nav.aap.utbetal.tilkjentytelse

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.HttpStatusCode
import javax.sql.DataSource


fun NormalOpenAPIRoute.registrerTilkjentYtelse(dataSource: DataSource) =

    route("/tilkjentytelse").post<Unit, Unit, TilkjentYtelseDto> { _, tilkjentYtelse ->
        TilkjentYtelseService().lagre(dataSource, tilkjentYtelse)
        respondWithStatus(HttpStatusCode.OK)
    }