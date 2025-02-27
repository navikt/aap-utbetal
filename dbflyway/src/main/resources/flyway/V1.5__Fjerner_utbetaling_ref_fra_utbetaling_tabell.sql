-- Fjerner utbetaling_ref. Bruker primærnøkkel fra utbetaling istedet.
ALTER TABLE UTBETALING DROP COLUMN UTBETALING_REF;
