package no.nav.aap.utbetal.migrering

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.utbetal.server.prometheus
import java.time.LocalDate
import java.util.*
import javax.sql.DataSource


fun main() {
    val utbetalDataSource = dataSourceForUtbetaling()
    val behandlingsflytDataSource = dataSourceForBehandlingsflyt()
    val data = hentUtbetalinger(utbetalDataSource)

    println("Fant ${data.count()} utbetalinger")

    val migreringsdata = mutableMapOf<UUID, Migreringsdata>()
    data.forEachIndexed { index, utbetaling ->
        val perioder = hentMeldeperioder(utbetaling, behandlingsflytDataSource)
        print("\r")
        print("$index/${data.count()} utbetalinger")
        System.out.flush()
        migreringsdata[utbetaling.uid] = Migreringsdata(
            saksnummer = utbetaling.saksnummer,
            behandlingsref = utbetaling.behandlingsref,
            minFom = utbetaling.minFom,
            maxTom = utbetaling.maxTom,
            meldekortPerioder = perioder.meldekortPerioder,
        )
    }

    val json = DefaultJsonMapper.toJson(migreringsdata)

    println(json)
}


private fun hentUtbetalinger(utbetalDataSource: DataSource): List<DataFraUtbetaling> {
    return utbetalDataSource.transaction(readOnly = true) { conn ->
        conn.queryList("""
            select 
                u.utbetaling_ref,
                u.saksnummer,
                u.behandling_ref,
                min(lower(up.periode)) fom,
                max(upper(up.periode)) tom
            from 
                utbetaling u,
                utbetalingsperiode up
            where 
                u.id = up.utbetaling_id
            group by u.id
            
        """.trimIndent()) {
            setRowMapper { row ->
                DataFraUtbetaling(
                    uid = row.getUUID("utbetaling_ref"),
                    saksnummer = row.getString("saksnummer"),
                    behandlingsref = row.getUUID("behandling_ref"),
                    minFom = row.getLocalDate("fom"),
                    maxTom = row.getLocalDate("tom"),
                )
            }
        }
    }
}


private fun hentMeldeperioder(dataFraUtbetaling: DataFraUtbetaling, behandlingsflytDataSource: DataSource): DataFraBehandlingsflyt {
    val query = """
            SELECT periode FROM MELDEPERIODE 
            JOIN MELDEPERIODE_GRUNNLAG ON MELDEPERIODE.meldeperiodegrunnlag_id = MELDEPERIODE_GRUNNLAG.id
            JOIN BEHANDLING ON MELDEPERIODE_GRUNNLAG.behandling_id = behandling.id
            WHERE BEHANDLING.referanse = ? AND aktiv = true
            order by periode
        """.trimIndent()

    val perioder =  behandlingsflytDataSource.transaction(readOnly = true) { conn ->
        conn.queryList(query) {
            setParams {
                setUUID(1, dataFraUtbetaling.behandlingsref)
            }
            setRowMapper { row ->
                row.getPeriode("periode")
            }
        }
    }
    val minMaxPeriode = Periode(dataFraUtbetaling.minFom, dataFraUtbetaling.maxTom)
    return DataFraBehandlingsflyt(
        dataFraUtbetaling.behandlingsref, perioder.filter {it.overlapper(minMaxPeriode)}
    )
}

data class Migreringsdata(
    val saksnummer: String,
    val behandlingsref: UUID,
    val minFom: LocalDate,
    val maxTom: LocalDate,
    val meldekortPerioder: List<Periode>
)

data class DataFraBehandlingsflyt(
    val behandlingsref: UUID,
    val meldekortPerioder: List<Periode>
)

data class DataFraUtbetaling(
    val uid: UUID,
    val saksnummer: String,
    val behandlingsref: UUID,
    val minFom: LocalDate,
    val maxTom: LocalDate,
)

private fun dataSourceForUtbetaling() =
    HikariDataSource(HikariConfig().apply {
        jdbcUrl = "jdbc:postgresql://localhost:10000/utbetal?user=frode.lindas@nav.no"
        username = "frode.lindas@nav.no"
        password = ""
        maximumPoolSize = 1
        minimumIdle = 1
        driverClassName = "org.postgresql.Driver"
        connectionTestQuery = "SELECT 1"
        metricRegistry = prometheus
    })


private fun dataSourceForBehandlingsflyt() =
    HikariDataSource(HikariConfig().apply {
        jdbcUrl = "jdbc:postgresql://localhost:10001/behandlingsflyt?user=frode.lindas@nav.no"
        username = "frode.lindas@nav.no"
        password = ""
        maximumPoolSize = 1
        minimumIdle = 1
        driverClassName = "org.postgresql.Driver"
        connectionTestQuery = "SELECT 1"
        metricRegistry = prometheus
    })

