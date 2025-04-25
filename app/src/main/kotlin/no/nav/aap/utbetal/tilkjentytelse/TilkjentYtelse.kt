package no.nav.aap.utbetal.tilkjentytelse

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.utbetal.felles.YtelseDetaljer
import no.nav.aap.utbetal.kodeverk.AvventÅrsak
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class TilkjentYtelse(
    val id: Long? = null,
    val saksnummer: Saksnummer,
    val behandlingsreferanse: UUID,
    val forrigeBehandlingsreferanse: UUID? = null,
    val personIdent: String,
    val vedtakstidspunkt: LocalDateTime,
    val beslutterId: String,
    val saksbehandlerId: String,
    val perioder: List<TilkjentYtelsePeriode>,
    val avvent: TilkjentYtelseAvvent? = null
)

data class TilkjentYtelsePeriode(
    val periode: Periode,
    val detaljer: YtelseDetaljer
)

data class TilkjentYtelseAvvent(
    val fom: LocalDate,
    val tom: LocalDate,
    val overføres: LocalDate,
    val årsak: AvventÅrsak? = null,
    val feilregistrering: Boolean = false,
)