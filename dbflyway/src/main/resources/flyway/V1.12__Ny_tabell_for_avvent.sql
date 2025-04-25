-- Avvent koblet til tilkjent ytelse
CREATE TABLE TILKJENT_YTELSE_AVVENT
(
    ID                      BIGSERIAL       NOT NULL PRIMARY KEY,
    TILKJENT_YTELSE_ID      BIGINT          NOT NULL REFERENCES TILKJENT_YTELSE (ID),
    PERIODE                 DATERANGE       NOT NULL,
    OVERFORES               DATE            NOT NULL,
    ARSAK                   VARCHAR(20)     ,
    FEILREGISTRERING        BOOLEAN         NOT NULL
);
