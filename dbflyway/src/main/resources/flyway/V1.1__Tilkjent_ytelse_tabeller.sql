CREATE TABLE TILKJENT_YTELSE
(
    ID                      BIGSERIAL       NOT NULL PRIMARY KEY,
    BEHANDLING_REF          UUID            NOT NULL,
    FORRIGE_BEHANDLING_REF  UUID
);


CREATE TABLE TILKJENT_PERIODE
(
    ID                 BIGSERIAL       NOT NULL PRIMARY KEY,
    PERIODE            DATERANGE       NOT NULL,
    DAGSATS            NUMERIC(21, 0)  NOT NULL,
    GRUNNLAG           NUMERIC(21, 0)  NOT NULL,
    GRADERING          SMALLINT        NOT NULL,
    GRUNNBELOP         NUMERIC(21)     NOT NULL,
    ANTALL_BARN        SMALLINT        NOT NULL,
    BARNETILLEGG       NUMERIC(21)     NOT NULL,
    GRUNNLAGSFAKTOR    NUMERIC(21, 10) NOT NULL,
    BARNETILLEGGSATS   NUMERIC         NOT NULL,
    TILKJENT_YTELSE_ID BIGINT          NOT NULL REFERENCES TILKJENT_YTELSE (ID)
);