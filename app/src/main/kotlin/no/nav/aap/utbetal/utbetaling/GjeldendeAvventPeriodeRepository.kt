package no.nav.aap.utbetal.utbetaling

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDateTime

class GjeldendeAvventPeriodeRepository(private val connection: DBConnection) {

    fun lagre(gjeldendeAvventPeriode: GjeldendeAvventPeriode) {
        deaktiverGjeldeAvventPeriode(gjeldendeAvventPeriode.sakUtbetalingId)
        val sql = """
            INSERT INTO GJELDENDE_AVVENT_PERIODE(SAK_UTBETALING_ID, PERIODE, AKTIV) VALUES (?, ?::daterange, TRUE)
        """.trimIndent()

        connection.execute(sql) {
            setParams {
                setLong(1, gjeldendeAvventPeriode.sakUtbetalingId)
                setPeriode(2, gjeldendeAvventPeriode.periode)
            }
        }
    }

    //NB: Denne er laget for migrering, og kan fjernes når migrering er gjort.
    fun lagre(gjeldendeAvventPeriode: GjeldendeAvventPeriode, vedtakstidspunkt: LocalDateTime) {
        deaktiverGjeldeAvventPeriode(gjeldendeAvventPeriode.sakUtbetalingId)
        val sql = """
            INSERT INTO GJELDENDE_AVVENT_PERIODE(SAK_UTBETALING_ID, PERIODE, AKTIV, OPPRETTET_TID) VALUES (?, ?::daterange, TRUE, ?)
        """.trimIndent()

        connection.execute(sql) {
            setParams {
                setLong(1, gjeldendeAvventPeriode.sakUtbetalingId)
                setPeriode(2, gjeldendeAvventPeriode.periode)
                setLocalDateTime(3, vedtakstidspunkt)
            }
        }
    }

    fun hentGjeldendeAvventPeriode(sakUtbetalingId: Long): GjeldendeAvventPeriode? {
        val sql = """
            SELECT SAK_UTBETALING_ID, PERIODE
            FROM GJELDENDE_AVVENT_PERIODE
            WHERE SAK_UTBETALING_ID = ? AND AKTIV = TRUE
        """.trimIndent()

        return connection.queryFirstOrNull(sql) {
            setParams {
                setLong(1, sakUtbetalingId)
            }
            setRowMapper { row ->
                GjeldendeAvventPeriode(
                    sakUtbetalingId = row.getLong("SAK_UTBETALING_ID"),
                    periode = row.getPeriode("PERIODE")
                )
            }
        }
    }

    private fun deaktiverGjeldeAvventPeriode(sakUtbetalingId: Long) {
        val sql = """
            UPDATE GJELDENDE_AVVENT_PERIODE
            SET AKTIV = FALSE
            WHERE SAK_UTBETALING_ID = ? AND AKTIV = TRUE
        """.trimIndent()

        connection.execute(sql) {
            setParams {
                setLong(1, sakUtbetalingId)

            }
        }
    }

}

data class GjeldendeAvventPeriode(
    val sakUtbetalingId: Long,
    val periode: Periode,
)
