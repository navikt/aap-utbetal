# aap-utbetal

Applikasjon som sørger for at tilkjent ytelse fra behandlingsflyt blir omgjort til utbetalinger som sendt til oppdrag via helved-utbetaling.

### API-dokumentasjon

APIene er dokumentert med Swagger.
* Lokalt: http://localhost:8080/swagger-ui/index.html
* Testmiljø: https://aap-utbetal.intern.dev.nav.no/swagger-ui/index.html


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

### Hovedfunksjoner

#### #1: Vedtak på førstegangsbehandling

```mermaid
sequenceDiagram
Behandlingsflyt->>Utbetal: Ny tilkjent ytelse (vedtak)
Utbetal->>Database: Opprett rad i SAK_UTBETALING
Utbetal->>Database: Lagre tilkjent ytelse
Utbetal->>Utbetal: Opprett Helved-utbetaling
Utbetal->>Database: Lagre Helved-utbetaling
Utbetal->>Utbetalmotor: Opprett overfør utbetaling task(saksnummer)

```

#### #2: Vedtak på revurdering
```mermaid
sequenceDiagram
    Behandlingsflyt->>Utbetal: Ny tilkjent ytelse (vedtak i revurdering)
    Utbetal->>Database: Hent rad fra SAK_UTBETALING
    Utbetal->>Database: Lagre tilkjent ytelse
    Utbetal->>Utbetal: Sjekk om ny tilkjent ytelse påvirker tidligere utbetalinger
    opt Påvirker tidligere utbetaling?
        Utbetal->>Utbetal: Opprett Helved-utbetaling for eventuell etterbetaling
        Utbetal->>Database: Lagre Helved-utbetaling
        Utbetal->>Utbetalmotor: Opprett overfør utbetaling task(saksnummer)
    end
```

#### #3: Overfør utbetaling til Helved-utbetaling

```mermaid
sequenceDiagram
Utbetalmotor->>Utbetal: Start overfør utbetaling(saksnummer)
Utbetal->>Database: Hent Helved-utbetaling
Utbetal->>HelvedUtbetaling: Send utbetaling
Utbetal->>Database: Oppdater status til SENDT(utbetalingId)
Utbetal->>Database: Sett dato for neste utbetaling i SAK_UTBETALING
```

#### #4: Finn nye utbetalinger som skal overføres

```mermaid
sequenceDiagram
Utbetalmotor->>Utbetal: Trigger daglig opprettelse av utbetalinger
Utbetal->>Utbetal: Finn alle saker hvor utbetalingsdato er passert
loop For hver sak hvor utbetalingsdato er passert
    Utbetal->>Utbetalmotor: Opprett task for sak som trenger utbetaling(saksnummer)
end
```

#### #5: Opprett utbetaling (for utbetaling hver 14. dag)
```mermaid
sequenceDiagram
Utbetalmotor->>Utbetal: Opprett utbetaling
Utbetal->>Database: Hent siste tilkjent ytelse
Utbetal->>Utbetal: Opprett Helved-utbetaling
Utbetal->>Database: Lagre Helved-utbetaling
Utbetal->>Utbetalmotor: Opprett overfør utbetaling task(saksnummer)
```


#### #6: Behandle kvittering

```mermaid
sequenceDiagram
Utbetalmotor->>Utbetal: Behandle kvitteringer (trigges hvert 15. min.)
Utbetal->>Database: Hent alle utbetalinger som mangler kvittering
loop For hver utbetaling som mangler kvittering
    Utbetal->>Utbetalmotor: Lag task for hver utbetaling som mangler kvittering(utbetalingId)
end
```
#### #7: Hent kvitteringer for utbetaling og oppdater database

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
