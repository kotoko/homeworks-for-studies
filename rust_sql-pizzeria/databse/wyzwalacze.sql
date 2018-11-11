CREATE OR REPLACE FUNCTION wyzwalacz_ilosc_w_magazynie_a() RETURNS trigger AS $$
BEGIN
	IF NEW.ilosc >= 0 THEN
		RETURN NEW;
	END IF;

	RAISE 'Oczekiwano nieujemnej ilości w magazynie';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER wyzwalacz_ilosc_w_magazynie_a
BEFORE INSERT OR UPDATE ON skladniki_w_magazynie
FOR EACH ROW EXECUTE PROCEDURE wyzwalacz_ilosc_w_magazynie_a();


CREATE OR REPLACE FUNCTION wyzwalacz_ilosc_w_magazynie_z() RETURNS trigger AS $$
BEGIN
	IF EXISTS
	(SELECT * FROM skladniki_w_magazynie S WHERE S.nazwa_skladnika = NEW.nazwa_skladnika)
	THEN
		RETURN NULL;
	ELSE
		RETURN NEW;
	END IF;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER wyzwalacz_ilosc_w_magazynie_z
BEFORE INSERT ON skladniki_w_magazynie
FOR EACH ROW EXECUTE PROCEDURE wyzwalacz_ilosc_w_magazynie_z();


CREATE OR REPLACE FUNCTION wyzwalacz_ilosc_w_skladnikach() RETURNS trigger AS $$
BEGIN
	IF NEW.ilosc >= 0 THEN
		RETURN NEW;
	END IF;

	RAISE 'Oczekiwano nieujemnej ilości w sładnikach pizzy';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER wyzwalacz_ilosc_w_skladnikach
BEFORE INSERT OR UPDATE ON skladniki_pizzy
FOR EACH ROW EXECUTE PROCEDURE wyzwalacz_ilosc_w_skladnikach();


CREATE OR REPLACE FUNCTION wyzwalacz_ilosc_w_zamowionych_pizzach() RETURNS trigger AS $$
BEGIN
	IF NEW.ilosc >= 0 THEN
		RETURN NEW;
	END IF;

	RAISE 'Oczekiwano nieujemnej ilości w zamówionych pizzach';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER wyzwalacz_ilosc_w_zamowionych_pizzach
BEFORE INSERT OR UPDATE ON zamowione_pizze
FOR EACH ROW EXECUTE PROCEDURE wyzwalacz_ilosc_w_zamowionych_pizzach();


CREATE OR REPLACE FUNCTION wyzwalacz_cena_pizzy() RETURNS trigger AS $$
BEGIN
	IF NEW.cena >= 0 OR NEW.cena IS NULL THEN
		RETURN NEW;
	END IF;

	RAISE 'Oczekiwano nieujemnej lub NULL ceny w pizzach';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER wyzwalacz_cena_pizzy
BEFORE INSERT OR UPDATE ON pizze
FOR EACH ROW EXECUTE PROCEDURE wyzwalacz_cena_pizzy();
