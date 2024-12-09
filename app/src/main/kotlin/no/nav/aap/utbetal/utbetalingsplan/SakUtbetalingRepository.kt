package no.nav.aap.utbetal.utbetalingsplan

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection
import java.time.LocalDateTime

data class SakUtbetaling(
    val id: Long? = null,
    val saksnummer: Saksnummer,
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
)

class SakUtbetalingRepository(val connection: DBConnection) {

    fun  lagre(sakUtbetaling: SakUtbetaling): Long {
        val sql = """
            INSERT INTO SAK_UTBETALING (SAKSNUMMER, OPPRETTET_TIDSPUNKT) VALUES (?, ?)
        """.trimIndent()

        return connection.executeReturnKey(sql) {
            setParams {
                setString(1, sakUtbetaling.saksnummer.toString())
                setLocalDateTime(2, sakUtbetaling.opprettetTidspunkt)
            }
        }
    }

    fun hent(saksnummer: Saksnummer): SakUtbetaling? {
        val sql = """
            SELECT ID, SAKSNUMMER, OPPRETTET_TIDSPUNKT FROM SAK_UTBETALING WHERE SAKSNUMMER = ?
        """.trimIndent()

        return connection.queryFirstOrNull(sql) {
            setParams {
                setString(1, saksnummer.toString())
            }
            setRowMapper { row ->
                SakUtbetaling(
                    id = row.getLong("ID"),
                    saksnummer = Saksnummer(row.getString("Saksnummer")),
                    opprettetTidspunkt = row.getLocalDateTime("opprettet_tidspunkt")
                )
            }
        }
    }

}