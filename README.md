# aap-utbetal


### API-dokumentasjon

APIene er dokumentert med Swagger: http://localhost:8080/swagger-ui/index.html


### Kontekstdiagram
```mermaid
graph TD
    Behandlingsflyt--Oppdaterer med tilkjent ytelse<br/> ved vedtak-->Utbetal((Utbetal))
    Selvbetjening--Hent utbetalinger-->Utbetal((Utbetal))
    Saksbehandling--Hent utbetalinger<br/> og status-->Utbetal((Utbetal))
    Saksbehandling--Hent simulering-->Utbetal((Utbetal))
    Utbetal--Lagre tilkjent ytelse og<br/> opprett task for utbetaling-->DB[(Database)]
    Utbetal--Lagre sendt utbetaling og<br/> opprett task for hent kvittering-->DB[(Database)]
    Utbetal--Oppdaterer status<br/> for utbetaling-->DB[(Database)]
    Utbetal--Simuler utbetaling-->Helved-utbetaling
    Utbetal--Oppdaterer med tilkjent ytelse<br/> fra første endring-->Helved-utbetaling
    Utbetal--Sjekker status<br/> for utbetaling-->Helved-utbetaling
```

### Lokalt utviklingsmiljø:

Applikasjonen aap-utbetal bruker test-containers for integrasjonstest med databasen.
En Docker-container er derfor nødvendig.
For macOS og Linux anbefaler vi Colima. Det kan være nødvendig med et par tilpasninger:

* `export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=$HOME/.colima/docker.sock`
* `export DOCKER_HOST=unix://$TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE`
* `export TESTCONTAINERS_RYUK_DISABLED=true`
