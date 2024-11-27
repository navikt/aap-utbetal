package no.nav.aap.utbetal.tilkjentytelse

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import java.util.UUID

class TilkjentYtelseRepository(private val connection: DBConnection) {

    fun lagre(tilkjentYtelse: TilkjentYtelseDto) {
        val sqlInsertTilkjentYtelse = """
            INSERT INTO TILKJENT_YTELSE 
                (BEHANDLING_REF, FORRIGE_BEHANDLING_REF)
                VALUES(?, ?)
        """.trimIndent()

        val tilkjentYtelseId = connection.executeReturnKey(sqlInsertTilkjentYtelse) {
            setParams {
                setUUID(1, tilkjentYtelse.behandlingsreferanse)
                setUUID(2, tilkjentYtelse.forrigeBehandlingsreferanse)
            }
        }

        lagre(tilkjentYtelseId, tilkjentYtelse.perioder)
    }


    private fun lagre(tilkjentYtelseId: Long, tilkjentPerioder: List<TilkjentYtelsePeriodeDto>) {
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
                    TILKJENT_YTELSE_ID
                )
                VALUES (?::daterange, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()


        connection.executeBatch(sqlInsertTilkjentPeriode, tilkjentPerioder) {
            setParams {
                setPeriode(1, Periode(it.fom, it.tom))
                setBigDecimal(2, it.detaljer.dagsats)
                setBigDecimal(3, it.detaljer.grunnlag)
                setBigDecimal(4, it.detaljer.gradering)
                setBigDecimal(5, it.detaljer.grunnbeløp)
                setInt(6, it.detaljer.antallBarn)
                setBigDecimal(7, it.detaljer.barnetillegg)
                setBigDecimal(8, it.detaljer.grunnlagsfaktor)
                setBigDecimal(9, it.detaljer.barnetilleggsats)
                setBigDecimal(10, it.detaljer.redusertDagsats)
                setLong(11, tilkjentYtelseId)
            }
        }
    }


    fun hent(behandlingReferanse: UUID): TilkjentYtelseDto? {
        val selectTilkjentYtelse = """
            SELECT 
                ID,
                BEHANDLING_REF,
                FORRIGE_BEHANDLING_REF
            FROM 
                TILKJENT_YTELSE
            WHERE
                BEHANDLING_REF = ?
        """.trimIndent()


        val idOgTilkjentYtelse = connection.queryFirstOrNull<Pair<Long, TilkjentYtelseDto>>(selectTilkjentYtelse) {
            setParams {
                setUUID(1, behandlingReferanse)
            }
            setRowMapper { row ->
                row.getLong("ID") to TilkjentYtelseDto(
                    behandlingsreferanse = row.getUUID("BEHANDLING_REF"),
                    forrigeBehandlingsreferanse = row.getUUIDOrNull("FORRIGE_BEHANDLING_REF"),
                    listOf()
                )
            }
        }
        if (idOgTilkjentYtelse != null) {
            val (tilkjentYtelseId, tilkjentYtelse) = idOgTilkjentYtelse
            return tilkjentYtelse.copy(perioder = hentTilkjentePerioder(tilkjentYtelseId))
        } else {
            return null
        }
    }

    private fun hentTilkjentePerioder(tilkjentYtelseId: Long): List<TilkjentYtelsePeriodeDto> {
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
                REDUSERT_DAGSATS
            FROM TILKJENT_PERIODE
            WHERE TILKJENT_YTELSE_ID = ? 
        """.trimIndent()

        return connection.queryList<TilkjentYtelsePeriodeDto>(selectTilkjentePerioder) {
            setParams {
                setLong(1, tilkjentYtelseId)
            }
            setRowMapper { row ->

                val periode = row.getPeriode("PERIODE")
                TilkjentYtelsePeriodeDto(
                    fom = periode.fom,
                    tom = periode.tom,
                    detaljer = TilkjentYtelseDetaljerDto(
                        dagsats = row.getBigDecimal("DAGSATS"),
                        grunnlag = row.getBigDecimal("GRUNNLAG"),
                        gradering = row.getBigDecimal("GRADERING"),
                        grunnbeløp = row.getBigDecimal("GRUNNLAG"),
                        antallBarn = row.getInt("ANTALL_BARN"),
                        barnetillegg = row.getBigDecimal("BARNETILLEGG"),
                        grunnlagsfaktor = row.getBigDecimal("GRUNNLAGSFAKTOR"),
                        barnetilleggsats = row.getBigDecimal("BARNETILLEGGSATS"),
                        redusertDagsats = row.getBigDecimal("REDUSERT_DAGSATS"),
                    )
                )
            }
        }
    }

}