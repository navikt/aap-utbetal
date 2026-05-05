package no.nav.aap.utbetal.tilkjentytelse

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.utbetal.hendelse.konsument.Status
import no.nav.aap.utbetal.hendelse.konsument.UtbetalingLinje
import no.nav.aap.utbetal.hendelse.konsument.UtbetalingStatusHendelse
import java.util.*

class UtbetalingStatusRepository(private val connection: DBConnection) {

    fun oppdaterUtbetalingStatus(tilkjentYtelseId: Long, utbetalingStatusHendelse: UtbetalingStatusHendelse) {
        slettTidligereStatus(tilkjentYtelseId)
        lagreUtbetalingStatus(tilkjentYtelseId, utbetalingStatusHendelse)
    }

    fun hent(behandlingRef: UUID): UtbetalingStatus? {
        val utbetalingStatus = hentUtbetalingStatus(behandlingRef) ?: return null
        return utbetalingStatus.copy(linjer = hentUtbetalingStatusLinjer(utbetalingStatus.id))
    }

    fun erAlleUtbetalingerBekreftet(saksnummer: Saksnummer): Boolean {
        val hentUtbetalingStatusSql = """
            SELECT 
                US.ID,                                                                        
                US.STATUS,
                TY.ID AS TILKJENT_YTELSE_ID
            FROM UTBETALING_STATUS US
            JOIN TILKJENT_YTELSE TY ON TY.ID = US.TILKJENT_YTELSE_ID
            WHERE 
                TY.SAKSNUMMER = ? AND
                US.AKTIV = TRUE
            ORDER BY US.ID DESC
            LIMIT 1
        """.trimIndent()


        val utbetalinger = connection.queryList<UtbetalingStatusLight>(hentUtbetalingStatusSql) {
            setParams {
                setString(1, saksnummer.toString())
            }
            setRowMapper { row ->
                UtbetalingStatusLight(
                    id = row.getLong("ID"),
                    status = row.getEnum("STATUS"),
                    tilkjentYtelseId = row.getLong("TILKJENT_YTELSE_ID"),
                )
            }
        }
        return utbetalinger.all {it.status == Status.OK}
    }


    private fun hentUtbetalingStatus(behandlingRef: UUID): UtbetalingStatus? {
        val hentUtbetalingStatusSql = """
            SELECT 
                US.ID,                                                                        
                US.STATUS, 
                US.HTTP_STATUS_KODE, 
                US.FEILMELDING, 
                US.DOKUMENTASJON_REFERANSE
            FROM UTBETALING_STATUS US
            JOIN TILKJENT_YTELSE TY ON TY.ID = US.TILKJENT_YTELSE_ID
            WHERE 
                TY.BEHANDLING_REF = ? AND
                US.AKTIV = TRUE
            ORDER BY US.ID DESC
            LIMIT 1
        """.trimIndent()


        return connection.queryFirstOrNull<UtbetalingStatus>(hentUtbetalingStatusSql) {
            setParams {
                setUUID(1, behandlingRef)
            }
            setRowMapper { row ->
                UtbetalingStatus(
                    id = row.getLong("ID"),
                    status = row.getEnum("STATUS"),
                    httpStatusKode = row.getIntOrNull("HTTP_STATUS_KODE"),
                    feilMelding = row.getStringOrNull("FEILMELDING"),
                    dokumentasjonReferanse = row.getStringOrNull("DOKUMENTASJON_REFERANSE"),
                    linjer = listOf() //Legger til linjene senere, da det krever en egen spørring for å hente ut linjene til en utbetaling status
                )
            }
        }
    }

    private fun hentUtbetalingStatusLinjer(utbetalingStatusId: Long): List<UtbetalingStatusLinje> {
        val hentUtbetalingStatusLinjerSql = """
            SELECT 
                PERIODE, 
                VEDTAKSSATS, 
                BELOP, 
                KLASSEKODE
            FROM UTBETALING_STATUS_LINJE
            WHERE 
                UTBETALING_STATUS_ID = ?
            ORDER BY PERIODE
        """.trimIndent()

        return connection.queryList(hentUtbetalingStatusLinjerSql) {
            setParams {
                setLong(1, utbetalingStatusId)
            }
            setRowMapper { row ->
                UtbetalingStatusLinje(
                    periode = row.getPeriode("PERIODE"),
                    vedtakssats = row.getIntOrNull("VEDTAKSSATS"),
                    beløp = row.getInt("BELOP"),
                    klassekode = row.getString("KLASSEKODE"),
                )
            }
        }
    }

    private fun slettTidligereStatus(tilkjentYtelseId: Long) {
        val sql = """
            UPDATE UTBETALING_STATUS
            SET AKTIV = FALSE
            WHERE TILKJENT_YTELSE_ID = ?
        """.trimIndent()
        connection.execute(sql) {
            setParams {
                setLong(1, tilkjentYtelseId)
            }
        }
    }

    private fun lagreUtbetalingStatus(tilkjentYtelseId: Long, utbetalingStatusHendelse: UtbetalingStatusHendelse) {
        val sql = """
            INSERT INTO UTBETALING_STATUS (TILKJENT_YTELSE_ID, STATUS, HTTP_STATUS_KODE, FEILMELDING, DOKUMENTASJON_REFERANSE)
            VALUES (?, ?, ?, ?, ?)
        """.trimIndent()
        val utbetalingStatusId = connection.executeReturnKey(sql) {
            setParams {
                setLong(1, tilkjentYtelseId)
                setEnumName(2, utbetalingStatusHendelse.status)
                setInt(3, utbetalingStatusHendelse.error?.statusKode)
                setString(4, utbetalingStatusHendelse.error?.msg)
                setString(5, utbetalingStatusHendelse.error?.doc)
            }
        }
        lagreUtbetalingLinjer(utbetalingStatusId, utbetalingStatusHendelse.detaljer.linjer)
    }

    private fun lagreUtbetalingLinjer(utbetalingStatusId: Long, utbetalingLinjer: List<UtbetalingLinje>) {
        val sql = """
            INSERT INTO UTBETALING_STATUS_LINJE (UTBETALING_STATUS_ID, PERIODE, VEDTAKSSATS, BELOP, KLASSEKODE)
            VALUES (?, ?::daterange, ?, ?, ?)
        """.trimIndent()

        connection.executeBatch(sql, utbetalingLinjer) {
            setParams {
                setLong(1, utbetalingStatusId)
                setPeriode(2, it.periode)
                setInt(3, it.vedtakssats?.toInt())
                setInt(4, it.beløp.toInt())
                setString(5, it.klassekode)
            }
        }
    }

}

data class UtbetalingStatusLight(
    val id: Long,
    val status: Status,
    val tilkjentYtelseId: Long,
)

data class UtbetalingStatus(
    val id: Long,
    val status: Status,
    val httpStatusKode: Int?,
    val feilMelding: String?,
    val dokumentasjonReferanse: String?,
    val linjer: List<UtbetalingStatusLinje>,
)

data class UtbetalingStatusLinje(
    val periode: Periode,
    val vedtakssats: Int?,
    val beløp: Int,
    val klassekode: String,
)
