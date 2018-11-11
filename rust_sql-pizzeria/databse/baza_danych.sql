CREATE TABLE klienci (
    id                         SERIAL NOT NULL,
    nr_rachunku_bankowego      TEXT,
    nr_telefonu                TEXT
);

ALTER TABLE klienci ADD CONSTRAINT klienci_pk PRIMARY KEY ( id );

CREATE TABLE pizze (
    nazwa     TEXT NOT NULL,
    cena      DOUBLE PRECISION,
    przepis   TEXT
);

ALTER TABLE pizze ADD CONSTRAINT pizze_pk PRIMARY KEY ( nazwa );

CREATE TABLE skladniki_pizzy (
    nazwa_pizzy                             TEXT NOT NULL,
    nazwa_skladnika                         TEXT NOT NULL,
    ilosc                                   DOUBLE PRECISION NOT NULL
);

ALTER TABLE skladniki_pizzy ADD CONSTRAINT skladniki_pizzy_pk PRIMARY KEY ( nazwa_pizzy, nazwa_skladnika );

CREATE TABLE skladniki_w_magazynie (
    nazwa_skladnika   TEXT NOT NULL,
    ilosc             DOUBLE PRECISION NOT NULL
);

ALTER TABLE skladniki_w_magazynie ADD CONSTRAINT skladniki_w_magazynie_pk PRIMARY KEY ( nazwa_skladnika );

CREATE TABLE zamowienia (
    id                SERIAL NOT NULL,
    data_zlozenia     TIMESTAMP NOT NULL,
    data_realizacji   TIMESTAMP,
    id_klienta        INTEGER NOT NULL
);

ALTER TABLE zamowienia ADD CONSTRAINT zamowienia_pk PRIMARY KEY ( id );

CREATE TABLE zamowione_pizze (
    id_zamowienia   INTEGER NOT NULL,
    nazwa_pizzy     TEXT NOT NULL,
    cena            DOUBLE PRECISION NOT NULL,
    ilosc           DOUBLE PRECISION NOT NULL
);

ALTER TABLE zamowione_pizze ADD CONSTRAINT zamowione_pizze_pk PRIMARY KEY ( id_zamowienia, nazwa_pizzy );

ALTER TABLE skladniki_pizzy
    ADD CONSTRAINT skladniki_pizzy_pizza_fk FOREIGN KEY ( nazwa_pizzy )
        REFERENCES pizze ( nazwa ) ON DELETE CASCADE;

ALTER TABLE zamowienia
    ADD CONSTRAINT zamowienia_klienci_fk FOREIGN KEY ( id_klienta )
        REFERENCES klienci ( id ) ON DELETE CASCADE;

-- usuwam, bo po usunieciu pizzy chce nadal miec informacaje o zamowieniu
--ALTER TABLE zamowione_pizze
--    ADD CONSTRAINT zamowione_pizze_pizza_fk FOREIGN KEY ( nazwa_pizzy )
--        REFERENCES pizze ( nazwa );

ALTER TABLE zamowione_pizze
    ADD CONSTRAINT zamowione_pizze_zamowienia_fk FOREIGN KEY ( id_zamowienia )
        REFERENCES zamowienia ( id ) ON DELETE CASCADE;

