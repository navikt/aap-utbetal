package no.nav.aap.utbetal.utbetaling

import no.nav.aap.komponenter.dbconnect.DBConnection
import java.time.LocalDateTime

data class Utbetalingsmelding(
    val id: Long? = null,
    val sakUtbetalingId: Long,
    val tilkjentYtelseId: Long,
    val melding: String,
    val opprettet: LocalDateTime,
)

class UtbetalingsmeldingRepository(private val connection: DBConnection) {

    fun lagre(utbetalingsmelding: Utbetalingsmelding): Long {
        val sql = """
            INSERT INTO UTBETALINGSMELDING(SAK_UTBETALING_ID, TILKJENT_YTELSE_ID, MELDING, OPPRETTET_TID)
            VALUES (?, ?, ?::JSONB, ?)
        """.trimIndent()

        return connection.executeReturnKey(sql) {
            setParams {
                setLong(1, utbetalingsmelding.sakUtbetalingId)
                setLong(2, utbetalingsmelding.tilkjentYtelseId)
                setString(3, utbetalingsmelding.melding)
                setLocalDateTime(4, utbetalingsmelding.opprettet)
            }
        }
    }

    fun hent(tilkjentYtelseId: Long): Utbetalingsmelding {
        val sql = """
            SELECT ID, SAK_UTBETALING_ID, TILKJENT_YTELSE_ID, MELDING, OPPRETTET_TID 
            FROM UTBETALINGSMELDING
            WHERE TILKJENT_YTELSE_ID = ?
        """.trimIndent()

        return connection.queryFirst<Utbetalingsmelding>(sql) {
            setParams {
                setLong(1, tilkjentYtelseId)
            }
            setRowMapper { row ->
                Utbetalingsmelding(
                    id = row.getLong("ID"),
                    sakUtbetalingId = row.getLong("SAK_UTBETALING_ID"),
                    tilkjentYtelseId = row.getLong("TILKJENT_YTELSE_ID"),
                    melding = row.getString("MELDING"),
                    opprettet = row.getLocalDateTime("OPPRETTET_TID")
                )
            }
        }
    }

}