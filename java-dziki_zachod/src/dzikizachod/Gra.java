package dzikizachod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Klasa implementująca mechanikę gry.
 */
public class Gra {
	/** Stół z informacjami dla graczy. */
	private Stół stół;
	/** Karty do dobierania. */
	private PulaAkcji talia;
	/** Karty odrzucone. */
	private PulaAkcji odrzucone;
	/** Karty odrzucone, które nie będą wtasowane ponownie. */
	private PulaAkcji odrzuconeNaZawsze;
	/** Gracze w grze. */
	private ArrayList<Gracz> gracze;
	/** Lista ruchów aktualnego gracza. */
	private ArrayList<Ruch> ruchyAktualnegoGracza;
	/** Limit tur na grę. */
	private final int limitTur;
	/** Czy dynamit jest w grze? */
	private boolean dynamitWGrze;
	/** Numer aktualnego gracza [1..n]. */
	private Integer aktualnyGracz;


	/** Konstruktor nowej gry. */
	public Gra() {
		this.stół = new Stół(this);
		this.talia = new PulaAkcji();
		this.odrzucone = new PulaAkcji();
		this.odrzuconeNaZawsze = new PulaAkcji();
		this.ruchyAktualnegoGracza = new ArrayList<>();
		this.limitTur = 42;
	}

	/**
	 * Funkcja dla numeru gracza zwraca indeks tego gracza w tablicy.
	 * @param gracz numer gracza
	 * @return indeks w tablicy
	 */
	int gracz(Integer gracz) {
		return gracz - 1;
	}

	/**
	 * Funkcja dla zadanych parametrów przeprowadza rozgrywkę.
	 * Liczba akcji w puli musi być większa/równa 5 * liczba_graczy.
	 * @param gracze kolekcja graczy
	 * @param talia pula akcji do gry
	 */
	public void rozgrywka(Collection<Gracz> gracze, PulaAkcji talia) {
		this.gracze = new ArrayList<>(gracze);
		this.talia = new PulaAkcji(talia);

		this.wypiszLinię("** START");

		try {
			this.przeprowadźRozgrywkę();
		}
		catch(KoniecGry wyjątek) {
			this.wypiszLinię("** KONIEC");
			this.wypiszLinię(wyjątek.wynik.toString(), 1);
		}
	}

	/**
	 * Dobierz karty z talii.
	 * Funkcja wywoływana przez obiekty typu gracz na początku tury.
	 * @param liczba liczba kart do dobrania
	 * @return lista kart
	 */
	List<Karta> dajKarty(Integer liczba) {
		List<Karta> karty = new ArrayList<>();

		if(this.talia.ileKart() < liczba) {
			this.przetasujTalię();
		}

		for(int i = 0; i < liczba; ++i) {
			karty.add(this.talia.weźKartę());
		}

		return karty;
	}

	/**
	 * Zagraj kartę ULECZ.
	 * Funkcja wywoływana przez obiekty typu gracz.
	 * @param karta karta typu ULECZ
	 * @param gracz numer gracza
	 */
	void uleczŻycie(Karta karta, Integer gracz) {
		this.odrzucone.dodaj(karta);
		this.gracze.get(this.gracz(gracz)).dodajŻycie(1);

		Ruch ruch = new Ruch(this.aktualnyGracz, Akcja.ULECZ, gracz);
		this.ruchyAktualnegoGracza.add(ruch);
		this.stół.dodajRuch(ruch);
	}

	/**
	 * Zagraj kartę STRZEL.
	 * Funkcja wywoływana przez obiekty typu gracz.
	 * @param karta karta typu STRZEL
	 * @param cel numer gracza, w którego strzela
	 * @throws KoniecGry gdy po strzale zginie gracz i zostaną spełnione warunki zwycięstwa
	 */
	void strzel(Karta karta, Integer cel) throws KoniecGry {
		this.odrzucone.dodaj(karta);
		this.gracze.get(this.gracz(cel)).odejmijŻycie(1);

		Ruch ruch = new Ruch(this.aktualnyGracz, Akcja.STRZEL, cel);
		this.ruchyAktualnegoGracza.add(ruch);
		this.stół.dodajRuch(ruch);

		if(!this.stół.czyGraczŻyje(cel)) {
			this.odrzucone.dodaj(this.gracze.get(this.gracz(cel)).oddajWszystkieKarty());
			this.stół.umarłGracz(this.aktualnyGracz, cel);
		}

		sprawdźCzyKtośWygrał();
	}

	/**
	 * Zagraj kartę ZASIEG_PLUS_JEDEN lub ZASIEG_PLUS_DWA.
	 * Funkcja wywoływana przez obiekty typu gracz.
	 * @param karta karta typu ZASIEG_PLUS_JEDEN lub ZASIEG_PLUS_DWA
	 */
	void dodajZasięg(Karta karta) {
		Ruch ruch;
		if(karta.akcja() == Akcja.ZASIEG_PLUS_DWA) {
			ruch = new Ruch(this.aktualnyGracz, Akcja.ZASIEG_PLUS_DWA, this.aktualnyGracz);
			this.gracze.get(this.gracz(this.aktualnyGracz)).dodajZasięg(2);
		}
		else {
			ruch = new Ruch(this.aktualnyGracz, Akcja.ZASIEG_PLUS_JEDEN, this.aktualnyGracz);
			this.gracze.get(this.gracz(this.aktualnyGracz)).dodajZasięg(1);
		}

		this.ruchyAktualnegoGracza.add(ruch);
		this.stół.dodajRuch(ruch);

		this.odrzucone.dodaj(karta);
	}

	/**
	 * Zagraj kartę DYNAMIT.
	 * Funkcja wywoływana przez obiekty typu gracz.
	 * @param karta karta typu DYNAMIT
	 */
	void rzućDynamit(Karta karta) {
		Ruch ruch = new Ruch(this.aktualnyGracz, Akcja.DYNAMIT, this.stół.graczNaLewo());
		this.ruchyAktualnegoGracza.add(ruch);
		this.stół.dodajRuch(ruch);

		this.dynamitWGrze = true;
		this.odrzuconeNaZawsze.dodaj(karta);
	}

	/**
	 * Funkcja rzuca kością K6 na dynamit.
	 * @return czy dynamit wybuchł?
	 */
	private boolean rzućKostkąK6() {
		// Źródło: http://stackoverflow.com/a/16624834
		return 1 == (int)(Math.random() * ((6 - 1) + 1) + 1);
	}

	/**
	 * Funkcja wkłada karty odrzucone do talii i tasuje talię.
	 */
	private void przetasujTalię() {
		List<Karta> karty = this.odrzucone.weźWszystkie();

		this.talia.dodaj(karty);
		this.talia.potasuj();
	}

	/**
	 * Funkcja sprawdza czy są spełnione warunki zwycięstwa którejś ze stron.
	 * @throws KoniecGry jeśli ktoś wygrał podnosi wyjątek
	 */
	private void sprawdźCzyKtośWygrał() throws KoniecGry {
		boolean bandyciŻyją = false;

		for(Gracz gracz : this.gracze) {
			if(gracz instanceof Szeryf && !gracz.czyŻyję()) {
				throw new KoniecGry(Wynik.WYGRALI_BANDYCI);
			}

			if(gracz instanceof Bandyta && gracz.czyŻyję()) {
				bandyciŻyją = true;
			}
		}

		if(!bandyciŻyją) {
			throw new KoniecGry(Wynik.WYGRAL_SZERYF);
		}
	}

	/**
	 * Funkcja losuje kolejność graczy i ustawia ich przy stole (nadaje im numery).
	 * Szeryf ma zawsze numer 1.
	 */
	private void ustawGraczy() {
		Gracz szeryf = null;

		for(Gracz gracz : this.gracze) {
			if(gracz instanceof Szeryf) {
				szeryf = gracz;
			}
		}

		this.gracze.remove(szeryf);
		Collections.shuffle(this.gracze);
		this.gracze.add(0,szeryf);

		int i = 1;
		for(Gracz gracz : this.gracze) {
			gracz.zagrajWGrę(this, new WidokStołu(this.stół), i);
			i++;
		}
	}

	/**
	 * Funkcja wypisuje linię napisu z odpowiednim wcięciem
	 * @param napis napis do wypisania
	 * @param wcięcie wcięcie na początku linii
	 */
	private void wypiszLinię(String napis, int wcięcie) {
		StringBuilder string = new StringBuilder("");

		while(wcięcie > 0) {
			string.append("  ");
			wcięcie--;
		}

		string.append(napis);

		System.out.println(string.toString());
	}

	/**
	 * Funkcja wypisuje pustą linię.
	 */
	private void wypiszLinię() {
		this.wypiszLinię("", 0);
	}

	/**
	 * Funkcja wypisuje linię napisu.
	 * @param napis napis do wypisania
	 */
	private void wypiszLinię(String napis){
		this.wypiszLinię(napis, 0);
	}

	/**
	 * Funkcja wypisuje status wszystkich graczy.
	 * @param wcięcie wcięcie na początku linii
	 */
	private void wypiszStatusGraczy(int wcięcie) {
		Integer i = 1;

		this.wypiszLinię("Gracze:", wcięcie);

		for(Gracz gracz : this.gracze) {
			if(gracz.czyŻyję()) {
				this.wypiszLinię(i + ": " + gracz.tożsamość()
						+ " (liczba żyć: " + gracz.życie() +")", wcięcie + 1);
			}
			else {
				this.wypiszLinię(i + ": X (" + gracz.tożsamość() + ")", wcięcie + 1);
			}

			++i;
		}

		this.wypiszLinię();
	}

	/**
	 * Funkcja wypisuje akcje na ręce aktualnego gracza.
	 * @param wcięcie wcięcie na początku linii
	 */
	private void wypiszAkcjeGracza(int wcięcie) {
		boolean pierwszy = true;
		StringBuilder napis = new StringBuilder("");
		napis.append("Akcje: [");

		for(Karta karta: gracze.get(this.gracz(this.aktualnyGracz)).karty()) {
			if(!pierwszy) {
				napis.append(", ");
			}

			napis.append(karta.toString());
			pierwszy = false;
		}

		napis.append("]");

		this.wypiszLinię(napis.toString(), wcięcie);
	}

	/**
	 * Funkcja wypisuje ruchy aktualnego gracza.
	 * @param wcięcie wcięcie na początku linii
	 */
	private void wypiszRuchyGracza(int wcięcie) {
		this.wypiszLinię("Ruchy:", wcięcie);

		for(Ruch ruch : this.ruchyAktualnegoGracza) {
			wypiszLinię(ruch.toString(), wcięcie + 1);
		}

		wypiszLinię();
	}

	/**
	 * Funkcja wypisuje ruchy gracza, gdy gracza zabije dynamit.
	 * (Nowa funkcja, bo trzeba wypisać to idiotyczne "MARTWY".)
	 * @param wcięcie wcięcie na początku linii
	 */
	private void wypiszRuchyGraczaPoWybuchu(int wcięcie) {
		this.wypiszLinię("Ruchy:", wcięcie);
		this.wypiszLinię("MARTWY", wcięcie + 1);
		wypiszLinię();
	}

	/**
	 * Jeśli dynamit jest w grze wypisuje linijkę czy wybuchł czy przechodzi dalej.
	 * Jeśli nie ma dynamitu w grze nic nie wypisuje.
	 * @param wybuchł czy dynamit wybuchł?
	 * @param wcięcie wcięcie na początku linii
	 */
	private void wypiszDynamit(boolean wybuchł, int wcięcie) {
		if(this.dynamitWGrze) {
			if(wybuchł) {
				this.wypiszLinię("Dynamit: WYBUCHŁ", wcięcie);
			}
			else {
				this.wypiszLinię("Dynamit: PRZECHODZI DALEJ", wcięcie);
			}
		}
	}

	/**
	 * Funkcja wypisuje numer tury.
	 * @param numer numer tury
	 * @param wcięcie wcięcie na początku linii
	 */
	private void wypiszNumerTury(Integer numer, int wcięcie) {
		this.wypiszLinię("** Tura " + numer, wcięcie);
	}

	/**
	 * Funkcja przeprowadza rozgrywkę.
	 * @throws KoniecGry gdy gra się skończy podnosi wyjątek
	 */
	private void przeprowadźRozgrywkę() throws KoniecGry {
		boolean dynamitWybuchł;
		int numerTury;

		this.talia.potasuj();

		ustawGraczy();
		this.stół.początekGry(this.gracze.toArray(new Gracz[gracze.size()]),this.limitTur, this.odrzucone);

		numerTury = 1;
		this.aktualnyGracz = 1;
		this.dynamitWGrze = false;

		this.wypiszStatusGraczy(1);

		while(numerTury <= this.limitTur) {
			this.stół.ustawTurę(numerTury);
			this.wypiszNumerTury(numerTury, 0);

			for(int i = 1; i <= this.stół.liczbaGraczy(); ++i) {
				this.stół.ustawAktualnegoGracza(this.aktualnyGracz);
				this.wypiszLinię("Gracz " + this.aktualnyGracz + " "
						+ "(" + this.gracze.get(this.gracz(this.aktualnyGracz)).tożsamość() + "):", 1 );

				if(this.gracze.get(this.gracz(this.aktualnyGracz)).czyŻyję()) {
					this.gracze.get(this.gracz(this.aktualnyGracz)).dobierzKarty();
					this.wypiszAkcjeGracza(2);

					if(this.dynamitWGrze) {
						dynamitWybuchł = this.rzućKostkąK6();

						this.wypiszDynamit(dynamitWybuchł, 2);

						if(dynamitWybuchł) {
							this.gracze.get(this.gracz(this.aktualnyGracz)).odejmijŻycie(3);
							this.dynamitWGrze = false;

							if(!this.gracze.get(this.gracz(this.aktualnyGracz)).czyŻyję()) {
								this.stół.umarłGraczOdDynamitu(this.aktualnyGracz);

								this.wypiszRuchyGraczaPoWybuchu(2);
								this.wypiszStatusGraczy(1);

								this.sprawdźCzyKtośWygrał();

								this.aktualnyGracz = this.stół.graczNaLewo(this.aktualnyGracz);
								this.ruchyAktualnegoGracza.clear();

								continue;
							}
						}
					}

					try {
						this.sprawdźCzyKtośWygrał();
						this.gracze.get(this.gracz(this.aktualnyGracz)).wykonajSwojąTurę();
					}
					catch(KoniecGry koniec) {
						this.wypiszRuchyGracza(2);
						this.wypiszStatusGraczy(1);

						throw koniec;
					}

					this.wypiszRuchyGracza(2);
				}
				else {
					this.wypiszLinię("MARTWY", 2);
					this.wypiszLinię();
				}

				this.wypiszStatusGraczy(1);

				this.aktualnyGracz = this.stół.graczNaLewo(this.aktualnyGracz);
				this.ruchyAktualnegoGracza.clear();
			}

			numerTury++;
		}

		throw new KoniecGry(Wynik.REMIS);
	}

}
