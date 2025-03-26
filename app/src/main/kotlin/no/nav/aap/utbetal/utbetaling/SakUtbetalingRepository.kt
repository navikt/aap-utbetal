package no.nav.aap.utbetal.utbetaling

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import java.time.LocalDateTime
import java.util.UUID

data class SakUtbetaling(
    val id: Long? = null,
    val saksnummer: Saksnummer,
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),
)

data class SakOgBehandling(
    val saksnummer: Saksnummer,
    val behandlingRef: UUID
)

class SakUtbetalingRepository(val connection: DBConnection) {

    private val alleSakUtbetalingFelter = "ID, SAKSNUMMER, OPPRETTET_TIDSPUNKT"

    fun lagre(sakUtbetaling: SakUtbetaling): Long {
        val sql = "INSERT INTO SAK_UTBETALING (SAKSNUMMER, OPPRETTET_TIDSPUNKT) VALUES (?, ?)"

        return connection.executeReturnKey(sql) {
            setParams {
                setString(1, sakUtbetaling.saksnummer.toString())
                setLocalDateTime(2, sakUtbetaling.opprettetTidspunkt)
            }
        }
    }

    fun avslutt(saksnummer: Saksnummer) {
        val sql = "UPDATE SAK_UTBETALING SET AKTIV = FALSE WHERE SAKSNUMMER = ?"
        return connection.execute(sql) {
            setParams {
                setString(1, saksnummer.toString())
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

    fun finnÅpneSakerOgSisteBehandling(): List<SakOgBehandling> {
        val finnSisteTilkjentYtelseSql = """
            SELECT
                TY.SAKSNUMMER,
                TY.BEHANDLING_REF
            FROM 
                TILKJENT_YTELSE TY,
                SAK_UTBETALING SU
            WHERE
                TY.BEHANDLING_REF NOT IN (
                    SELECT FORRIGE_BEHANDLING_REF FROM TILKJENT_YTELSE WHERE SAKSNUMMER = TY.SAKSNUMMER AND FORRIGE_BEHANDLING_REF IS NOT NULL
                ) AND
                TY.SAKSNUMMER = SU.SAKSNUMMER
        """.trimIndent()

        return connection.queryList<SakOgBehandling>(finnSisteTilkjentYtelseSql) {
            setRowMapper { row ->
                SakOgBehandling(
                    saksnummer = Saksnummer(row.getString("SAKSNUMMER")),
                    behandlingRef = row.getUUID("BEHANDLING_REF")
                )
            }
        }
    }

    fun finnAktiveSaker(): List<SakUtbetaling> {
        val sql = "SELECT $alleSakUtbetalingFelter FROM SAK_UTBETALING WHERE AKTIV = TRUE"

        return connection.queryList(sql) {
            setRowMapper { it.tilSakUtbetaling() }
        }

    }

    private fun Row.tilSakUtbetaling() =
        SakUtbetaling(
            id = this.getLong("ID"),
            saksnummer = Saksnummer(this.getString("SAKSNUMMER")),
            opprettetTidspunkt = this.getLocalDateTime("OPPRETTET_TIDSPUNKT")
        )


}