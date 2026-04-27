package no.nav.aap.utbetal.test

import no.nav.aap.utbetal.klienter.helved.Utbetaling
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

data class HelvedKall(
    val metode: String,
    val utbetalingRef: UUID,
    val utbetaling: Utbetaling,
)

class Fakes : AutoCloseable{
    private val log: Logger = LoggerFactory.getLogger(Fakes::class.java)
    private val azure = FakeServer(module = { azureFake() }, port = 8081)
    val utbetalinger = ConcurrentHashMap<UUID, Utbetaling>()
    val kall: MutableList<HelvedKall> = CopyOnWriteArrayList()
    private val helvedUtbetaling = FakeServer(module = {helvedUtbetalingFake(utbetalinger, kall)})
    init {
        Thread.currentThread().setUncaughtExceptionHandler { _, e -> log.error("Uhåndtert feil", e) }
        // Azure
        System.setProperty("azure.openid.config.token.endpoint", "http://localhost:${azure.port()}/token")
        System.setProperty("azure.app.client.id", "behandlingsflyt")
        System.setProperty("azure.app.client.secret", "")
        System.setProperty("azure.openid.config.jwks.uri", "http://localhost:${azure.port()}/jwks")
        System.setProperty("azure.openid.config.issuer", "behandlingsflyt")
        System.setProperty("integrasjon.utsjekk.url", "http://localhost:${helvedUtbetaling.port()}/")
        System.setProperty("integrasjon.utsjekk.scope", "helvedUtbetaling")
    }

    override fun close() {
        azure.stop()
        helvedUtbetaling.stop()
    }

}