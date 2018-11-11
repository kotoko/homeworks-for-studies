CREATE OR REPLACE FUNCTION dodaj_pizze_menu(
	_nazwa pizze.nazwa%TYPE,
	_cena pizze.cena%TYPE,
	_przepis pizze.przepis%TYPE,
--	_nazwy_skladnikow skladniki_pizzy.nazwa_skladnika%TYPE[],
--	_ilosci_skladnikow skladniki_pizzy.ilosc%TYPE[]
	_nazwy_skladnikow TEXT[],
	_ilosci_skladnikow DOUBLE PRECISION[]
	) RETURNS void AS $$
DECLARE
	i INTEGER;
BEGIN
	IF array_length(_nazwy_skladnikow, 1) != array_length(_ilosci_skladnikow, 1) THEN
		RAISE 'Oczekiwano równej długości list nazw i ilości składników';
	END IF;

	INSERT INTO pizze(nazwa, cena, przepis) VALUES(_nazwa, _cena, _przepis);

	FOR i IN array_lower(_nazwy_skladnikow, 1) .. array_upper(_nazwy_skladnikow, 1) LOOP
		INSERT INTO skladniki_pizzy(nazwa_pizzy, nazwa_skladnika, ilosc)
		VALUES(_nazwa, _nazwy_skladnikow[i], _ilosci_skladnikow[i]);

		INSERT INTO skladniki_w_magazynie(nazwa_skladnika, ilosc)
		VALUES(_nazwy_skladnikow[i], 0);
	END LOOP;
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION usun_pizze_menu(_nazwa pizze.nazwa%TYPE) RETURNS void AS $$
BEGIN
	DELETE FROM pizze WHERE nazwa = _nazwa;
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION dodaj_klienta(
	_nr_rachunku_bankowego klienci.nr_rachunku_bankowego%TYPE,
	_nr_telefonu klienci.nr_telefonu%TYPE
	) RETURNS INTEGER AS $$
DECLARE
	_id INTEGER;
BEGIN
	INSERT INTO klienci(nr_rachunku_bankowego, nr_telefonu)
	VALUES(_nr_rachunku_bankowego, _nr_telefonu)
	RETURNING id INTO _id;

	RETURN _id;
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION usun_klienta(_id klienci.id%TYPE) RETURNS void AS $$
BEGIN
	DELETE FROM klienci WHERE id = _id;
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION dodaj_skladnik_magazyn(
	_nazwa skladniki_w_magazynie.nazwa_skladnika%TYPE,
	_ilosc skladniki_w_magazynie.ilosc%TYPE
	) RETURNS void AS $$
DECLARE
	n INTEGER;
BEGIN
	IF _ilosc >= 0 THEN

		n = (SELECT count(*) FROM skladniki_w_magazynie
		WHERE nazwa_skladnika = _nazwa);

		IF n = 0 THEN
			INSERT INTO skladniki_w_magazynie(nazwa_skladnika, ilosc)
			VALUES(_nazwa, _ilosc);
		ELSE
			UPDATE skladniki_w_magazynie SET ilosc = ilosc + _ilosc
			WHERE nazwa_skladnika = _nazwa;
		END IF;

	ELSE
		RAISE 'Oczekiwano nieujemnej ilości składnika';
	END IF;
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION usun_skladnik_magazyn(
	_nazwa skladniki_w_magazynie.nazwa_skladnika%TYPE,
	_ilosc skladniki_w_magazynie.ilosc%TYPE
	) RETURNS void AS $$
DECLARE
	n INTEGER;
BEGIN
	IF _ilosc >= 0 THEN

		n = (SELECT count(*) FROM skladniki_w_magazynie
		WHERE nazwa_skladnika = _nazwa);

		IF n = 0 THEN
			RAISE 'Oczekiwano składnika, który istnieje w magazynie';
		END IF;

		UPDATE skladniki_w_magazynie SET ilosc = ilosc - _ilosc
		WHERE nazwa_skladnika = _nazwa;

	ELSE
		RAISE 'Oczekiwano nieujemnej ilości składnika';
	END IF;
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION lista_skladnikow_pizzy(_nazwa skladniki_pizzy.nazwa_pizzy%TYPE)
RETURNS TABLE(nazwa_skladnika TEXT, ilosc DOUBLE PRECISION) AS $$
BEGIN
	RETURN QUERY SELECT nazwa_skladnika, ilosc FROM skladniki_pizzy
	WHERE nazwa_pizzy = _nazwa;
	RETURN;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION dodaj_skladowa_zamowienia(
	_id_zamowienia zamowione_pizze.id_zamowienia%TYPE,
	_nazwa_pizzy zamowione_pizze.nazwa_pizzy%TYPE,
	_cena zamowione_pizze.cena%TYPE,
	_ilosc zamowione_pizze.ilosc%TYPE
	) RETURNS void AS $$
DECLARE
	n INTEGER;
BEGIN
	n := (SELECT count(*) FROM zamowione_pizze
	WHERE id_zamowienia = _id_zamowienia AND nazwa_pizzy = _nazwa_pizzy);

	IF n = 0 THEN
		INSERT INTO zamowione_pizze(id_zamowienia, nazwa_pizzy, cena, ilosc)
		VALUES(_id_zamowienia, _nazwa_pizzy, _cena, _ilosc);
	ELSE
		UPDATE zamowione_pizze SET ilosc = ilosc + _ilosc
		WHERE id_zamowienia = _id_zamowienia AND nazwa_pizzy = _nazwa_pizzy;
	END IF;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION wez_skladniki_magazyn(
	_id_zamowienia zamowione_pizze.id_zamowienia%TYPE
	) RETURNS void AS $$
BEGIN
	UPDATE skladniki_w_magazynie MA SET ilosc = MA.ilosc - Z.ilosc * S.ilosc
	FROM skladniki_pizzy S, zamowione_pizze Z
	WHERE MA.nazwa_skladnika = S.nazwa_skladnika AND Z.nazwa_pizzy = S.nazwa_pizzy AND Z.id_zamowienia = _id_zamowienia;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION zloz_zamowienie(
	_klient klienci.id%TYPE,
--	_lista_pizz pizze.nazwa%TYPE[]
	_lista_pizz TEXT[]
	) RETURNS INTEGER AS $$
DECLARE
	_cena pizze.cena%TYPE;
	_cena_pizzy pizze.cena%TYPE;
	_pizza pizze.nazwa%TYPE;
	n INTEGER;
	_id zamowienia.id%TYPE;
	i INTEGER;
BEGIN
	_cena = (SELECT sum(pizze.cena) FROM unnest(_lista_pizz) AS lista
	INNER JOIN pizze ON lista = nazwa);

	n := (SELECT count(*) FROM unnest(_lista_pizz) AS lista
	INNER JOIN pizze ON lista = nazwa);

	IF n = array_length(_lista_pizz, 1) THEN
		INSERT INTO zamowienia(data_zlozenia, id_klienta) VALUES(now(), _klient) RETURNING id INTO _id;

		FOR i IN array_lower(_lista_pizz, 1) .. array_upper(_lista_pizz, 1) LOOP
			_cena_pizzy := (SELECT cena FROM pizze WHERE nazwa = _lista_pizz[i]);

			PERFORM dodaj_skladowa_zamowienia(
				_id,
				_lista_pizz[i],
				_cena_pizzy,
				1
			);
		END LOOP;

		PERFORM wez_skladniki_magazyn(_id);

		RETURN _id;
	ELSE
		RAISE 'Oczekiwano pizzy z menu, takiej że cena jest różna od NULL';
	END IF;

END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION dokoncz_zamowienie(_id zamowienia.id%TYPE) RETURNS void AS $$
BEGIN
	UPDATE zamowienia SET data_realizacji = now() WHERE id = _id AND data_realizacji IS NULL;
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION usun_zamowienie(_id zamowienia.id%TYPE) RETURNS void AS $$
BEGIN
	DELETE FROM zamowienia WHERE id = _id;
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION lista_zamowionych_pizz(_id zamowienia.id%TYPE)
RETURNS TABLE(
	nazwa_pizzy zamowione_pizze.nazwa_pizzy%TYPE,
	cena zamowione_pizze.cena%TYPE,
	ilosc zamowione_pizze.ilosc%TYPE
	) AS $$
BEGIN
	RETURN QUERY SELECT nazwa_pizzy, cena, ilosc FROM zamowione_pizze
	WHERE id_zamowienia = _id;
	RETURN;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION ustaw_przepis(_pizza pizze.nazwa%TYPE, _przepis pizze.przepis%TYPE) RETURNS void AS $$
BEGIN
	UPDATE pizze SET przepis = _przepis WHERE nazwa = _pizza;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION zwroc_przepis(_pizza pizze.nazwa%TYPE) RETURNS pizze.przepis%TYPE AS $$
BEGIN
	IF (SELECT count(*) FROM pizze WHERE nazwa = _pizza) = 1 THEN
		RETURN (SELECT przepis FROM pizze WHERE nazwa = _pizza LIMIT 1);
	END IF;

	RAISE 'Oczekiwano pizzy, która jest w menu';
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION ustaw_telefon(_id klienci.id%TYPE, _nr_telefonu klienci.nr_telefonu%TYPE)
RETURNS void AS $$
BEGIN
	UPDATE klienci SET nr_telefonu = _nr_telefonu WHERE id = _id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION ustaw_nr_rachunku_bankowego(
	_id klienci.id%TYPE,
	_nr_rachunku_bankowego klienci.nr_rachunku_bankowego%TYPE
	) RETURNS void AS $$
BEGIN
	UPDATE klienci SET nr_rachunku_bankowego = _nr_rachunku_bankowego WHERE id = _id;
END;
$$ LANGUAGE plpgsql;
