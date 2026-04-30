package no.nav.aap.utbetal.tilkjentytelse

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.utbetal.MigreringService
import no.nav.aap.utbetal.trekk.TrekkPostering
import no.nav.aap.utbetal.trekk.TrekkRepository
import no.nav.aap.utbetal.trekk.TrekkService
import no.nav.aap.utbetal.utbetaling.KvitteringService
import no.nav.aap.utbetal.utbetaling.SakUtbetalingRepository
import no.nav.aap.utbetal.utbetaling.Utbetaling
import no.nav.aap.utbetal.utbetaling.UtbetalingJobbService
import no.nav.aap.utbetal.utbetaling.UtbetalingLight
import no.nav.aap.utbetal.utbetaling.UtbetalingRepository
import no.nav.aap.utbetaling.UtbetalingStatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

enum class TilkjentYtelseResponse {
    /**
     * Kan ikke motta ny tilkjent ytelse fordi en tidligere utbetaling ikke er ferdigbehandlet.
     */
    LOCKED,

    /**
     * Har mottatt en duplikat innsending av tilkjent ytelse som er forskjellig fra den forrige.
     */
    CONFLICT,

    /**
     * Tilkjent ytelse er mottatt.
     */
    OK
}

class TilkjentYtelseService(private val connection: DBConnection) {

    private val log: Logger = LoggerFactory.getLogger(TilkjentYtelseService::class.java)

    fun håndterNyTilkjentYtelse(tilkjentYtelse: TilkjentYtelse): TilkjentYtelseResponse {
        // Send LOCKED respons dersom ikke alle kvitteringer er ok.
        if (!sjekkOmTidligereUtbetalingerHarFåttKvittering(tilkjentYtelse)) {
            return TilkjentYtelseResponse.LOCKED
        }

        //Lagre tilkjent ytelse dersom den ikke er duplikat
        val tilkjentYtelseRepo = TilkjentYtelseRepository(connection)

        val trekkPosteringer = beregnTrekkPosteringer(tilkjentYtelse)
        val oppdatertTilkjentYtelse = TilkjentYtelsePeriodeSplitter.splitt(tilkjentYtelse, trekkPosteringer)

        val eksisterendeTilkjentYtelse = tilkjentYtelseRepo.hent(tilkjentYtelse.behandlingsreferanse)
        if (eksisterendeTilkjentYtelse == null) {
            val sakUtbetalingId = lagre(oppdatertTilkjentYtelse)
            val migreringService = MigreringService()
            if (migreringService.skalTilNyttGrensesnitt(oppdatertTilkjentYtelse.personIdent)) {
                UtbetalingJobbService(connection).overførUtbetalingJobbPåNyttGrensesnitt(
                    sakUtbetalingId = sakUtbetalingId,
                    behandlingsreferanse = oppdatertTilkjentYtelse.behandlingsreferanse
                )
            } else {
                UtbetalingJobbService(connection).opprettUtbetalingJobb(
                    sakUtbetalingId = sakUtbetalingId,
                    behandlingsreferanse = oppdatertTilkjentYtelse.behandlingsreferanse
                )
            }
        } else {
            // Sjekk om duplikat ikke er lik, slik at det kan sendes Conflict http code til klienten
            if (!eksisterendeTilkjentYtelse.erLik(oppdatertTilkjentYtelse)) {
                log.info("Duplikatkontroll på innsending av tilkjent ytelse $eksisterendeTilkjentYtelse er ikke like $oppdatertTilkjentYtelse")
                return TilkjentYtelseResponse.CONFLICT
            }
        }
        return TilkjentYtelseResponse.OK
    }

    /***
     * Sjekker om alle tidligere utbetalinger for saksnummmer har fått kvittering OK.
     *
     * @param tilkjentYtelse ny tilkjent ytelse.
     *
     * @return true hvis alle kvitteringer er mottatt, ellers false.
     */
    private fun sjekkOmTidligereUtbetalingerHarFåttKvittering(tilkjentYtelse: TilkjentYtelse): Boolean {
        if (MigreringService().skalTilNyttGrensesnitt(tilkjentYtelse.personIdent)) {
            return UtbetalingStatusRepository(connection).erAlleUtbetalingerBekreftet(tilkjentYtelse.saksnummer)
        } else {
            val utbetalingRepo = UtbetalingRepository(connection)

            val utbetalingerForSak = utbetalingRepo.hent(tilkjentYtelse.saksnummer)
                .also { utbetalinger ->
                    //Prøv å hente alle manglende kvitteringer
                    utbetalinger.hentKvitteringerForSendteUtbetalinger()
                }

            return  utbetalingerForSak.all { it.utbetalingStatus == UtbetalingStatus.BEKREFTET }
        }
    }


    private fun beregnTrekkPosteringer(tilkjentYtelse: TilkjentYtelse): List<TrekkPostering> {
        val trekkRepo = TrekkRepository(connection)
        val trekkService = TrekkService(trekkRepo)
        trekkService.oppdaterTrekk(tilkjentYtelse)
        val trekkListe = trekkRepo.hentTrekk(tilkjentYtelse.saksnummer)
        return trekkListe.flatMap { it.posteringer }
    }


    private fun List<Utbetaling>.hentKvitteringerForSendteUtbetalinger() {
        val kvitteringService = KvitteringService(connection)
        filter { it.utbetalingStatus == UtbetalingStatus.SENDT }
            .forEach { utbetaling ->
                kvitteringService.sjekkKvittering(utbetaling.tilUtbetalingLight())
            }
    }

    private fun Utbetaling.tilUtbetalingLight() =
        UtbetalingLight(
            id = id!!,
            utbetalingRef = utbetalingRef,
            saksnummer = saksnummer,
            behandlingsreferanse = behandlingsreferanse,
            utbetalingStatus = utbetalingStatus,
            utbetalingOpprettet = utbetalingOversendt,
            utbetalingEndret = utbetalingEndret,
            versjon = versjon,
        )

    /**
     * Lagre tilkjent ytelse. Oppretter SakUtbetaling dersom det er første tilkjente ytelse for denne saken.
     *
     * @param tilkjentYtelse tilkjent ytelse som skal lagres
     *
     * @return SakUtbetaling sin id
     */
    private fun lagre(tilkjentYtelse: TilkjentYtelse): Long {
        val sakUtbetalingRepo = SakUtbetalingRepository(connection)
        val migrertTilKafka = MigreringService().skalTilNyttGrensesnitt(tilkjentYtelse.personIdent)
        val sakUtbetalingId = if (tilkjentYtelse.forrigeBehandlingsreferanse == null) {
            sakUtbetalingRepo.lagre(tilkjentYtelse.saksnummer, migrertTilKafka)
        } else {
            val sakUtbetaling = sakUtbetalingRepo.hent(tilkjentYtelse.saksnummer)
            if (sakUtbetaling != null) {
                sakUtbetaling.id!!
            } else {
                // Opprett SakUtbetaling dersom den ikke finnes.
                sakUtbetalingRepo.lagre(tilkjentYtelse.saksnummer, migrertTilKafka)
            }
        }
        TilkjentYtelseRepository(connection).lagreTilkjentYtelse(tilkjentYtelse)
        return sakUtbetalingId
    }


    /**
     * Sjekk om tilkjent ytelser er like. Kan ikke bruke vanlig equals siden bl.a. BigDecimal ikke fungerer så bra i sammenligning av data classes.
     */
    private fun TilkjentYtelse.erLik(tilkjentYtelse: TilkjentYtelse): Boolean {

        if (saksnummer != tilkjentYtelse.saksnummer) return false
        if (vedtakstidspunkt.avrundet() != tilkjentYtelse.vedtakstidspunkt.avrundet()) return false
        if (forrigeBehandlingsreferanse != tilkjentYtelse.forrigeBehandlingsreferanse) return false
        if (personIdent != tilkjentYtelse.personIdent) return false
        if (beslutterId != tilkjentYtelse.beslutterId) return false
        if (saksbehandlerId != tilkjentYtelse.saksbehandlerId) return false
        if (perioder.size != tilkjentYtelse.perioder.size) return false
        if (avvent != tilkjentYtelse.avvent) return false
        for (index in tilkjentYtelse.perioder.indices) {
            val periode1 = perioder[index]
            val periode2 = tilkjentYtelse.perioder[index]
            if (periode1.periode != periode2.periode) return false
            val detaljer1 = periode1.detaljer
            val detaljer2 = periode2.detaljer
            if (detaljer1.redusertDagsats.avrundet() != detaljer2.redusertDagsats.avrundet()) return false
            if (detaljer1.gradering.prosentverdi() != detaljer2.gradering.prosentverdi()) return false
            if (detaljer1.dagsats.avrundet() != detaljer2.dagsats.avrundet()) return false
            if (detaljer1.grunnlag.avrundet() != detaljer2.grunnlag.avrundet()) return false
            if (detaljer1.grunnlagsfaktor.compareTo(detaljer2.grunnlagsfaktor) != 0) return false
            if (detaljer1.grunnbeløp.avrundet() != detaljer2.grunnbeløp.avrundet()) return false
            if (detaljer1.antallBarn != detaljer2.antallBarn) return false
            if (detaljer1.barnetilleggsats.avrundet() != detaljer2.barnetilleggsats.avrundet()) return false
            if (detaljer1.barnetillegg.avrundet() != detaljer2.barnetillegg.avrundet()) return false
            if (detaljer1.barnepensjonDagsats.avrundet() != detaljer2.barnepensjonDagsats.avrundet()) return false
            if (detaljer1.utbetalingsdato != detaljer2.utbetalingsdato) return false
        }
        if (nyMeldeperiode != tilkjentYtelse.nyMeldeperiode) return false
        if (trekk.size != tilkjentYtelse.trekk.size) return false
        for (index in tilkjentYtelse.trekk.indices) {
            val periode1 = trekk[index]
            val periode2 = tilkjentYtelse.trekk[index]
            if (periode1.dato != periode2.dato) return false
            if (periode1.beløp != periode2.beløp) return false
        }
        return true
    }

    private fun Beløp.avrundet() = verdi.toLong()

    private fun LocalDateTime.avrundet() = truncatedTo(ChronoUnit.MILLIS)


}
