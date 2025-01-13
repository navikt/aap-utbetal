# aap-utbetal


### API-dokumentasjon

APIene er dokumentert med Swagger: http://localhost:8080/swagger-ui/index.html


### Kontekstdiagram
```mermaid
graph TD
    Behandlingsflyt--Oppdaterer med tilkjent ytelse ved vedtak-->Utbetal((Utbetal))
    Saksbehandling--Hent utbetalinger og status-->Utbetal((Utbetal))
    Saksbehandling--Hent simulering-->Utbetal((Utbetal))
    Utbetal--Lagre tilkjent ytelse-->DB[(Database)]
    Utbetal--Lagre sendt utbetaling-->DB[(Database)]
    Utbetal--Oppdaterer status for utbetaling-->DB[(Database)]
    Utbetal--Simuler utbetaling-->Helved-utbetaling
    Utbetal--Oppdaterer ved ny tilkjent ytelse-->Helved-utbetaling
    Utbetal--Sjekker status for utbetaling-->Helved-utbetaling
```

### Lokalt utviklingsmiljø:

Applikasjonen aap-utbetal bruker test-containers for integrasjonstest med databasen.
En Docker-container er derfor nødvendig.
For macOS og Linux anbefaler vi Colima. Det kan være nødvendig med et par tilpasninger:

* `export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=$HOME/.colima/docker.sock`
* `export DOCKER_HOST=unix://$TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE`
* `export TESTCONTAINERS_RYUK_DISABLED=true`
