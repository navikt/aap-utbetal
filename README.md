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


### Flyt

```mermaid
flowchart LR
    Behandlingslyt["`**Behandlingsflyt**
                    Sender tilkjent ytelse
                    til Utbetal
                    når vedtak iverksettes`"]
    Utbetal["`**Utbetal**
            Mottar tilkjent ytelse
            og lagrer denne og
            lager task for å utlede
            utbetalinger`"]

    OpprettUtbetalingUtfører["`**OpprettUtbetalingUtfører**
                            Deler opp utbetaling i eventuelle endinger
                            og nye utbetalinger. Utbetalingene lagres
                            og task for sending av utbetaling opprettes
                            per utbetaling. Status på utbetaling er nå OPPRETTET.`"]

    OverførTilØkonomiJobbUtfører["`**OverførTilØkonomiJobbUtfører**
                                Konverterer utbetaling til Helved-utbetaling
                                sitt format, og sender utbetalingene enten som
                                en ny utbetaling eller endring av eksisterende
                                utbetaling. Utbetalings status 
                                settes til SENDT.`"]                                     
                            
    Behandlingslyt --REST--> Utbetal --Motor--> OpprettUtbetalingUtfører --Motor---> OverførTilØkonomiJobbUtfører

    SjekkForNyeUtbetalingerUtfører["`**SjekkForNyeUtbetalingerUtfører**
                                    Finner alle åpne saker og opprett en
                                    OpprettUtbetalingUtfører task for alle
                                    siste behandling.`"]
                                    
    MotorDagligUtbetaling["CRON: Daglig trigging kl 04:00"]

    MotorDagligUtbetaling--Motor-->SjekkForNyeUtbetalingerUtfører --Motor--> OpprettUtbetalingUtfører

    SjekkKvitteringFraØkonomiUtfører["`**SjekkKvitteringFraØkonomiUtfører**
                                        Henter alle utbetalinger med status
                                        SENDT og prøver å hente status
                                        på disse fra Helved-utbetaling.
                                        Dersom det er mottatt kvittering,
                                        så settes status enten til
                                        BEKREFTET eller FEILET.`"]
    
    MotorHentKvitteringer["`CRON: Trigges hvert
                            10. minutt`"]

    MotorHentKvitteringer --Motor--> SjekkKvitteringFraØkonomiUtfører
```


### Hovedfunksjoner

#### #1: Mottar tilkjent ytelse fra behandlingsflyt ved veedtak

```mermaid
sequenceDiagram
Behandlingsflyt->>Utbetal: Ny tilkjent ytelse (vedtak)
Utbetal->>Utbetal: Sjekk om alle tidligere utbetalinger er bekreftet (ellers returner LOCKED)
Utbetal->>Utbetal: Sjekk at tilkjent ytelse ikke er duplikat (ellers returner CONFLICT)
Utbetal->>Database: Opprett rad i SAK_UTBETALING dersom den ikke eksisterer
Utbetal->>Database: Lagre tilkjent ytelse
Utbetal->>Utbetalmotor: Opprett task for å opprette utbetalinger(saksnummer, behandlingRef)

```

#### #2: Opprett utbetalinger for gitt behandling/tilkjent ytelse
```mermaid
sequenceDiagram
    Utbetalmotor->>Utbetal: Start opprett utbetalinger(saksnummer, behandlingRef)
    Utbetal->>Database: Hent tilkjent ytelse for behandling
    Utbetal->>Database: Hent sak-utbetaling for sak
    Utbetal->>Database: Hent tidligere utbetalinger for sak
    Utbetal->>Utbetal: Bygg tidslinje for tidligere utbetalinger
    Utbetal->>Utbetal: Beregn utbetalinger(endringer og ny) utifra tilkjent ytelse og tidligere utbetalinger
    Utbetal->>Database: Lagrer de beregnede utbetalingene
    Utbetal->>Utbetalmotor: Opprett tasker for sending av alle beregnede utbetalinger(utbetalingId)
```

#### #3: Overfør utbetaling til Helved-utbetaling

```mermaid
sequenceDiagram
Utbetalmotor->>Utbetal: Start overfør utbetaling(utbetalingId)
Utbetal->>Database: Hent utbetaling
Utbetal->>Utbetal: Konverter utbetaling til Helved datamodell
Utbetal->>HelvedUtbetaling: Send utbetaling (POST for ny, PUT for endring og DELETE for opphør)
Utbetal->>Database: Oppdater status til SENDT(utbetalingId) på utbetaling
```

#### #4: Finn nye utbetalinger som skal overføres

```mermaid
sequenceDiagram
Utbetalmotor->>Utbetal: Trigger daglig opprettelse av utbetalinger
Utbetal->>Database: Finn alle åpne saker og siste behandling
loop For hver sak 
    Utbetal->>Utbetalmotor: Opprett task for opprettelse av utbetalinger(saksnummer, behandlingRef)
end
```

#### #5: Behandle kvittering

```mermaid
sequenceDiagram
Utbetalmotor->>Utbetal: Behandle kvitteringer (trigges hvert 10. min.)
Utbetal->>Database: Hent alle utbetalinger som mangler kvittering
loop For hver utbetaling som mangler kvittering
    Utbetal->>HelvedUtbetaling: Hent status for utbetaling
    Utbetal->>Database: Oppdater status på utbetaling dersom OK eller FEILET
end
```

### Lokalt utviklingsmiljø:

Applikasjonen aap-utbetal bruker test-containers for integrasjonstest med databasen.
En Docker-container er derfor nødvendig.
For macOS og Linux anbefaler vi Colima. Det kan være nødvendig med et par tilpasninger:

* `export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=$HOME/.colima/docker.sock`
* `export DOCKER_HOST=unix://$TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE`
* `export TESTCONTAINERS_RYUK_DISABLED=true`
