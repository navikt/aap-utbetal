# aap-utbetal

Applikasjon som sørger for at tilkjent ytelse fra behandlingsflyt blir omgjort til utbetalinger som sendt til oppdrag via helved-utbetaling.

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
Utbetal->>Utbetalmotor: Opprett overfør utbetaling task(saksnummer)

```

#### Scenario #2: Vedtak på revurdering
```mermaid
sequenceDiagram
    Behandlingsflyt->>Utbetal: Ny tilkjent ytelse (vedtak i revurdering)
    Utbetal->>Database: Lagre tilkjent ytelse
```

#### Scenario #3: Overfør utbetaling til Helved-utbetaling

```mermaid
sequenceDiagram
Utbetalmotor->>Utbetal: Start overfør utbetaling(saksnummer)
Utbetal->>Database: Hent siste tilkjent ytelse
Utbetal->>Utbetal: Opprett utbetaling
Utbetal->>Database: Lagre utbetaling
Database-->>Utbetal: UtbetalingId
Utbetal->>Utbetal: Lag Helved utbetaling
Utbetal->>HelvedUtbetaling: Send utbetaling
Utbetal->>Database: Oppdater status til SENDT(utbetalingId)
Utbetal->>Database: Sett dato for neste utbetaling i SAK_UTBETALING
Utbetal->>Utbetalmotor: Opprett hent kvittering task(utbetalingId)
```


#### Scenario #4: Finn nye utbetalinger som skal overføres

```mermaid
sequenceDiagram
Utbetalmotor->>Utbetal: Trigger daglig opprettelse av utbetalinger
Utbetal->>Utbetal: Finn alle saker hvor utbetalingsdato er passert
Utbetal->>Utbetalmotor: Opprett tasker for alle saker som trenger utbetaling(saksnummer)
```

#### Scenario #5: Behandle kvittering

```mermaid
sequenceDiagram
Utbetalmotor->>Utbetal: Hent alle utbetalinger som mangler kvittering (trigges hvert 15. min.)
Utbetal->>Utbetalmotor: Lag task for hver utbetaling som mangler kvittering(utbetalingId)
```
#### Scenario #6: Hent kvitteringer for utbetaling og oppdater database

```mermaid
sequenceDiagram
Utbetalmotor->>Utbetal: Sjekk kvittering (utbetalingId)
Utbetal->>Helved-utbetaling: Sjekk status for utbetaling (utbetalingId)
Utbetal->>Database: Oppdater status på utbetaling dersom endret
```

### Lokalt utviklingsmiljø:

Applikasjonen aap-utbetal bruker test-containers for integrasjonstest med databasen.
En Docker-container er derfor nødvendig.
For macOS og Linux anbefaler vi Colima. Det kan være nødvendig med et par tilpasninger:

* `export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=$HOME/.colima/docker.sock`
* `export DOCKER_HOST=unix://$TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE`
* `export TESTCONTAINERS_RYUK_DISABLED=true`
