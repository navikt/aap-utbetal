package no.nav.aap.utbetal.tilkjentytelse

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.utbetal.felles.YtelseDetaljer
import java.math.BigDecimal
import java.util.UUID

class TilkjentYtelseRepository(private val connection: DBConnection) {

    fun lagre(tilkjentYtelse: TilkjentYtelse): Long {
        val sqlInsertTilkjentYtelse = """
            INSERT INTO TILKJENT_YTELSE 
                (SAKSNUMMER, BEHANDLING_REF, FORRIGE_BEHANDLING_REF, PERSON_IDENT, VEDTAKSTIDSPUNKT, BESLUTTER_ID, SAKSBEHANDLER_ID)
                VALUES(?, ? ,?, ?, ?, ?, ?)
        """.trimIndent()

        val tilkjentYtelseId = connection.executeReturnKey(sqlInsertTilkjentYtelse) {
            setParams {
                setString(1, tilkjentYtelse.saksnummer.toString())
                setUUID(2, tilkjentYtelse.behandlingsreferanse)
                setUUID(3, tilkjentYtelse.forrigeBehandlingsreferanse)
                setString(4, tilkjentYtelse.personIdent)
                setLocalDateTime(5, tilkjentYtelse.vedtakstidspunkt)
                setString(6, tilkjentYtelse.beslutterId)
                setString(7, tilkjentYtelse.saksbehandlerId)
            }
        }

        lagre(tilkjentYtelseId, tilkjentYtelse.perioder)

        return tilkjentYtelseId
    }


    private fun lagre(tilkjentYtelseId: Long, tilkjentPerioder: List<TilkjentYtelsePeriode>) {
        val sqlInsertTilkjentPeriode = """
            INSERT INTO TILKJENT_PERIODE
                (
                    PERIODE,
                    DAGSATS,           
                    GRUNNLAG,          
                    GRADERING,         
                    GRUNNBELOP,        
                    ANTALL_BARN,
                    BARNETILLEGG,  
                    GRUNNLAGSFAKTOR,
                    BARNETILLEGGSATS,
                    REDUSERT_DAGSATS,
                    TILKJENT_YTELSE_ID,
                    VENTEDAGER_SAMORDNING,
                    UTBETALINGSDATO
                )
                VALUES (?::daterange, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()


        connection.executeBatch(sqlInsertTilkjentPeriode, tilkjentPerioder) {
            setParams {
                setPeriode(1, it.periode)
                setBigDecimal(2, it.detaljer.dagsats.verdi())
                setBigDecimal(3, it.detaljer.grunnlag.verdi())
                setInt(4, it.detaljer.gradering.prosentverdi())
                setBigDecimal(5, it.detaljer.grunnbeløp.verdi())
                setInt(6, it.detaljer.antallBarn)
                setBigDecimal(7, it.detaljer.barnetillegg.verdi())
                setBigDecimal(8, it.detaljer.grunnlagsfaktor.verdi())
                setBigDecimal(9, it.detaljer.barnetilleggsats.verdi())
                setBigDecimal(10, it.detaljer.redusertDagsats.verdi())
                setLong(11, tilkjentYtelseId)
                setBoolean(12, it.detaljer.ventedagerSamordning)
                setLocalDate(13, it.detaljer.utbetalingsdato)
            }
        }
    }


    fun hent(behandlingReferanse: UUID): TilkjentYtelse? {
        val selectTilkjentYtelse = """
            SELECT 
                ID,
                SAKSNUMMER,
                BEHANDLING_REF,
                FORRIGE_BEHANDLING_REF,
                PERSON_IDENT,
                VEDTAKSTIDSPUNKT,
                BESLUTTER_ID,
                SAKSBEHANDLER_ID
            FROM 
                TILKJENT_YTELSE
            WHERE
                BEHANDLING_REF = ?
        """.trimIndent()


        val tilkjentYtelse = connection.queryFirstOrNull<TilkjentYtelse>(selectTilkjentYtelse) {
            setParams {
                setUUID(1, behandlingReferanse)
            }
            setRowMapper { row ->
                TilkjentYtelse(
                    id = row.getLong("ID"),
                    saksnummer = Saksnummer(row.getString("SAKSNUMMER")),
                    behandlingsreferanse = row.getUUID("BEHANDLING_REF"),
                    forrigeBehandlingsreferanse = row.getUUIDOrNull("FORRIGE_BEHANDLING_REF"),
                    personIdent = row.getString("PERSON_IDENT"),
                    vedtakstidspunkt = row.getLocalDateTime("VEDTAKSTIDSPUNKT"),
                    beslutterId = row.getString("BESLUTTER_ID"),
                    saksbehandlerId = row.getString("SAKSBEHANDLER_ID"),
                    listOf()
                )
            }
        }
        return tilkjentYtelse?.copy(perioder = hentTilkjentePerioder(tilkjentYtelse.id!!))
    }

    fun finnSisteTilkjentYtelse(saksnummer: Saksnummer): Long? {
        val finnSisteTilkjentYtelseSql = """
            SELECT
                ID
            FROM 
                TILKJENT_YTELSE
            WHERE
                SAKSNUMMER = ? AND
                BEHANDLING_REF NOT IN (
                    SELECT FORRIGE_BEHANDLING_REF FROM TILKJENT_YTELSE WHERE SAKSNUMMER = ? AND FORRIGE_BEHANDLING_REF IS NOT NULL
                )
        """.trimIndent()

        return connection.queryFirstOrNull<Long>(finnSisteTilkjentYtelseSql) {
            setParams {
                setString(1, saksnummer.toString())
                setString(2, saksnummer.toString())
            }
            setRowMapper { row -> row.getLong("ID") }
        }
    }



    private fun hentTilkjentePerioder(tilkjentYtelseId: Long): List<TilkjentYtelsePeriode> {
        val selectTilkjentePerioder = """
            SELECT 
                PERIODE,
                DAGSATS,           
                GRUNNLAG,          
                GRADERING,         
                GRUNNBELOP,        
                ANTALL_BARN,
                BARNETILLEGG,  
                GRUNNLAGSFAKTOR,
                BARNETILLEGGSATS,
                REDUSERT_DAGSATS,
                VENTEDAGER_SAMORDNING,
                UTBETALINGSDATO
            FROM TILKJENT_PERIODE
            WHERE TILKJENT_YTELSE_ID = ? 
        """.trimIndent()

        return connection.queryList<TilkjentYtelsePeriode>(selectTilkjentePerioder) {
            setParams {
                setLong(1, tilkjentYtelseId)
            }
            setRowMapper { row ->

                val periode = row.getPeriode("PERIODE")
                TilkjentYtelsePeriode(
                    periode = periode,
                    detaljer = YtelseDetaljer(
                        dagsats = Beløp(row.getBigDecimal("DAGSATS")),
                        grunnlag = Beløp(row.getBigDecimal("GRUNNLAG")),
                        gradering = Prosent.fraDesimal(row.getBigDecimal("GRADERING")),
                        grunnbeløp = Beløp(row.getBigDecimal("GRUNNBELOP")),
                        antallBarn = row.getInt("ANTALL_BARN"),
                        barnetillegg = Beløp(row.getBigDecimal("BARNETILLEGG")),
                        grunnlagsfaktor = GUnit(row.getBigDecimal("GRUNNLAGSFAKTOR")),
                        barnetilleggsats = Beløp(row.getBigDecimal("BARNETILLEGGSATS")),
                        redusertDagsats = Beløp(row.getBigDecimal("REDUSERT_DAGSATS")),
                        ventedagerSamordning = row.getBoolean("VENTEDAGER_SAMORDNING"),
                        utbetalingsdato = row.getLocalDate("UTBETALINGSDATO"),
                    )
                )
            }
        }
    }

}