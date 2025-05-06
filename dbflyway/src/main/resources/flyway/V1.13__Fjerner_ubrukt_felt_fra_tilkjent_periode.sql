-- Fjerner felt som skal erstartes med avvent utbetaling tabellen. Alle rader skal ha verdi false, så ingen behov for å migrere data.
ALTER TABLE TILKJENT_PERIODE DROP COLUMN VENTEDAGER_SAMORDNING;
