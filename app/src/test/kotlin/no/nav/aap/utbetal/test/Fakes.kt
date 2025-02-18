package no.nav.aap.utbetal.test

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Fakes() : AutoCloseable{
    private val log: Logger = LoggerFactory.getLogger(Fakes::class.java)
    private val azure = FakeServer(module = { azureFake() })
    private val helvedUtbetaling = FakeServer(module = {helvedUtbetalingFake()})
    init {
        Thread.currentThread().setUncaughtExceptionHandler { _, e -> log.error("Uhåndtert feil", e) }
        // Azure
        System.setProperty("azure.openid.config.token.endpoint", "http://localhost:${azure.port()}/token")
        System.setProperty("azure.app.client.id", "behandlingsflyt")
        System.setProperty("azure.app.client.secret", "")
        System.setProperty("azure.openid.config.jwks.uri", "http://localhost:${azure.port()}/jwks")
        System.setProperty("azure.openid.config.issuer", "behandlingsflyt")
        System.setProperty("integrasjon.helved-utbetaling.url", "http://localhost:${helvedUtbetaling.port()}/")
        System.setProperty("integrasjon.helved-utbetaling.scope", "helvedUtbetaling")
    }

    override fun close() {
        azure.stop()
        helvedUtbetaling.stop()
    }

}