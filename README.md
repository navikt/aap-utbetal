# aap-utbetal


### API-dokumentasjon

APIene er dokumentert med Swagger: http://localhost:8080/swagger-ui/index.html


### Kontekstdiagram
```mermaid
graph TD
    Behandlingsflyt--Oppdaterer med tilkjent ytelse<br/> ved vedtak-->Utbetal((Utbetal))
    Selvbetjening--Hent utbetalinger-->Utbetal
    Saksbehandling--Hent utbetalinger<br/> og status-->Utbetal
    Saksbehandling--Hent simulering-->Utbetal
    Utbetal--Lagre tilkjent ytelse og<br/> opprett task for utbetaling-->DB[(Database)]
    Utbetal--Lagre sendt utbetaling og<br/> opprett task for hent kvittering-->DB
    Utbetal--Oppdaterer status<br/> for utbetaling-->DB
    Utbetal--Simuler utbetaling-->Helved-utbetaling
    Utbetal--Oppdaterer med tilkjent ytelse<br/> fra første endring-->Helved-utbetaling
    Utbetal--Sjekker status<br/> for utbetaling-->Helved-utbetaling
    Utbetal--Start utbetaling ved<br/> åpen behandling-->Helved-utbetaling
```

### Scenarioer

#### Scenario #1: Vedtak på førstegangsbehandling

```mermaid
sequenceDiagram
Behandlingsflyt->>Utbetal: Ny tilkjent ytelse (vedtak)
Utbetal->>Database: Opprett rad i SAK_UTBETALING
Utbetal->>Database: Lagre tilkjent ytelse
Utbetal->>Utbetalmotor: Opprett overfør utbetaling task(utbetalingId)
Utbetalmotor->>Database: Lagre overfør utbetaling task(utbetalingId)
```

#### Scenario #2: Vedtak på revurdering
```mermaid
sequenceDiagram
    Behandlingsflyt->>Utbetal: Ny tilkjent ytelse (vedtak)
    Utbetal->>Database: Lagre tilkjent ytelse
```

#### Scenario #3: Overfør utbetaling til Helved-utbetaling

```mermaid
sequenceDiagram
Utbetalmotor->>Utbetal: Start overfør utbetaling(utbetalingId)
Utbetal->>Database: Hent siste tilkjent ytelse
Utbetal->>Utbetal: Opprett utbetaling
Utbetal->>Database: Lagre utbetaling
Database-->>Utbetal: UtbetalingId
Utbetal->>Database: Hent utbetaling(utbetalingId)
Utbetal->>Utbetal: Lag Helved utbetaling
Utbetal->>HelvedUtbetaling: Send utbetaling
Utbetal->>Database: Oppdater status til SENDT(utbetalingId)
Utbetal->>Utbetalmotor: Opprett hent kvittering task(utbetalingId)
Utbetalmotor->>Database: Lagre hent kvittering task
```

#### Scenario #4: Behandle kvittering

TODO

#### Scenario #5: Finn nye utbetalinger som skal oversides

TODO

#### Scenario #6: Hent kvitteringer for utbetaling og oppdater database

TODO


### Lokalt utviklingsmiljø:

Applikasjonen aap-utbetal bruker test-containers for integrasjonstest med databasen.
En Docker-container er derfor nødvendig.
For macOS og Linux anbefaler vi Colima. Det kan være nødvendig med et par tilpasninger:

* `export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=$HOME/.colima/docker.sock`
* `export DOCKER_HOST=unix://$TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE`
* `export TESTCONTAINERS_RYUK_DISABLED=true`
