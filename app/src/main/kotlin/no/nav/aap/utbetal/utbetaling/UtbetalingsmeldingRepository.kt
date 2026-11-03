package no.nav.aap.utbetal.utbetaling

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.utbetal.kodeverk.Status
import no.nav.aap.utbetal.kodeverk.UtbetalingsmeldingType
import java.time.LocalDateTime
import java.util.UUID

data class Utbetalingsmelding(
    val id: Long? = null,
    val sakUtbetalingId: Long,
    val tilkjentYtelseId: Long,
    val referanse: UUID,
    val utbetalingsmeldingType: UtbetalingsmeldingType,
    val melding: String,
    val opprettet: LocalDateTime = LocalDateTime.now(),
)

data class UtbetalingsmeldingMedStatus(
    val utbetalingsmelding: Utbetalingsmelding,
    val status: Status,
)

class UtbetalingsmeldingRepository(private val connection: DBConnection) {

    fun lagre(utbetalingsmelding: Utbetalingsmelding): Long {
        val sql = """
            INSERT INTO UTBETALINGSMELDING(SAK_UTBETALING_ID, TILKJENT_YTELSE_ID, REFERANSE, MELDINGSTYPE, MELDING, OPPRETTET_TID)
            VALUES (?, ?, ?, ?, ?::JSONB, ?)
        """.trimIndent()

        return connection.executeReturnKey(sql) {
            setParams {
                setLong(1, utbetalingsmelding.sakUtbetalingId)
                setLong(2, utbetalingsmelding.tilkjentYtelseId)
                setUUID(3, utbetalingsmelding.referanse)
                setEnumName(4, utbetalingsmelding.utbetalingsmeldingType)
                setString(5, utbetalingsmelding.melding)
                setLocalDateTime(6, utbetalingsmelding.opprettet)
            }
        }
    }

    fun hent(referanse: UUID): Utbetalingsmelding? {
        val sql = """
            SELECT ID, SAK_UTBETALING_ID, TILKJENT_YTELSE_ID, REFERANSE, MELDINGSTYPE, MELDING, OPPRETTET_TID 
            FROM UTBETALINGSMELDING
            WHERE REFERANSE = ?
        """.trimIndent()

        return connection.queryFirstOrNull<Utbetalingsmelding>(sql) {
            setParams {
                setUUID(1, referanse)
            }
            setRowMapper { row ->
                tilUtbetalingsmelding(row)
            }
        }
    }

    fun hent(saksnummer: Saksnummer): List<UtbetalingsmeldingMedStatus> {
        val sql = """
            SELECT 
                UM.ID,
                UM.SAK_UTBETALING_ID,
                UM.TILKJENT_YTELSE_ID,
                UM.REFERANSE,
                UM.MELDINGSTYPE,
                UM.MELDING,
                UM.OPPRETTET_TID,
                US.STATUS
            FROM UTBETALINGSMELDING UM, UTBETALING_STATUS US, SAK_UTBETALING SU
            WHERE
                UM.ID = US.UTBETALINGSMELDING_ID AND
                UM.SAK_UTBETALING_ID = SU.ID AND
                SU.SAKSNUMMER = ? AND
                SU.AKTIV = TRUE AND
                US.AKTIV = TRUE
            ORDER BY UM.OPPRETTET_TID
        """.trimIndent()

        return connection.queryList(sql) {
            setParams {
                setString(1, saksnummer.toString())
            }
            setRowMapper { row ->
                UtbetalingsmeldingMedStatus(
                    utbetalingsmelding = tilUtbetalingsmelding(row),
                    status = row.getEnum("status")
                )
            }
        }
    }

    private fun tilUtbetalingsmelding(row: Row) =
        Utbetalingsmelding(
            id = row.getLong("ID"),
            sakUtbetalingId = row.getLong("SAK_UTBETALING_ID"),
            tilkjentYtelseId = row.getLong("TILKJENT_YTELSE_ID"),
            referanse = row.getUUID("REFERANSE"),
            utbetalingsmeldingType = row.getEnum<UtbetalingsmeldingType>("MELDINGSTYPE"),
            melding = row.getString("MELDING"),
            opprettet = row.getLocalDateTime("OPPRETTET_TID")
        )

}