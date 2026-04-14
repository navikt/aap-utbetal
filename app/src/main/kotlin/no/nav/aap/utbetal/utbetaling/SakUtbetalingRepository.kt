package no.nav.aap.utbetal.utbetaling

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import java.time.LocalDateTime

data class SakUtbetaling(
    val id: Long? = null,
    val saksnummer: Saksnummer,
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
    val migrertTilKafka: LocalDateTime?,
)

class SakUtbetalingRepository(val connection: DBConnection) {

    private val alleSakUtbetalingFelter = "ID, SAKSNUMMER, OPPRETTET_TIDSPUNKT, MIGRERT_TIL_KAFKA"

    fun lagre(saksnummer: Saksnummer, migrertTilKafka: Boolean): Long {
        val sql = """
            INSERT INTO SAK_UTBETALING (SAKSNUMMER, OPPRETTET_TIDSPUNKT, MIGRERT_TIL_KAFKA) 
            VALUES (?, ?, ?)
        """.trimIndent()

        val nå = LocalDateTime.now()

        return connection.executeReturnKey(sql) {
            setParams {
                setString(1, saksnummer.toString())
                setLocalDateTime(2, nå)
                setLocalDateTime(3, if (migrertTilKafka) nå else null)
            }
        }
    }

    fun settMigrertTilKafka(saksnummer: Saksnummer) {
        val sql = "UPDATE SAK_UTBETALING SET MIGRERT_TIL_KAFKA = ? WHERE SAKSNUMMER = ?"
        return connection.execute(sql) {
            setParams {
                setLocalDateTime(1, LocalDateTime.now())
                setString(2, saksnummer.toString())
            }
        }
    }

    fun hent(saksnummer: Saksnummer): SakUtbetaling? {
        val sql = "SELECT $alleSakUtbetalingFelter FROM SAK_UTBETALING WHERE SAKSNUMMER = ? and AKTIV = TRUE"

        return connection.queryFirstOrNull(sql) {
            setParams {
                setString(1, saksnummer.toString())
            }
            setRowMapper { it.tilSakUtbetaling() }
        }
    }

    private fun Row.tilSakUtbetaling() =
        SakUtbetaling(
            id = this.getLong("ID"),
            saksnummer = Saksnummer(this.getString("SAKSNUMMER")),
            opprettetTidspunkt = this.getLocalDateTime("OPPRETTET_TIDSPUNKT"),
            migrertTilKafka = this.getLocalDateTimeOrNull("MIGRERT_TIL_KAFKA")
        )

}