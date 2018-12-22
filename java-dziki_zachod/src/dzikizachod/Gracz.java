package dzikizachod;

import java.util.ArrayList;
import java.util.List;

/**
 * Klasa gracza biorącego udział w grze.
 */
public abstract class Gracz {
	/** Gra, w którą gra gracz. */
	private Gra gra;
	/** Widok gracza. */
	private WidokGracza widokGracza;
	/** Widok stołu. */
	private WidokStołu stół;
	/** Tożsamość gracza. */
	private final Tożsamość tożsamość;
	/** Aktualne życie gracza. */
	private int życie;
	/** Maksymalne życie gracza. */
	private final int maksymalneŻycie;
	/** Numer gracza przy stole. */
	private Integer numerPrzyStole;
	/** Karty gracza. */
	private List<Karta> karty;
	/** Strategia gracza. */
	private Strategia strategia;
	/** Zasięg gracza. */
	private int zasięg;


	/**
	 * Konstruktor Gracza.
	 * @param tożsamość tożsamość gracza
	 * @param strategia strategia gracza
	 */
	Gracz(Tożsamość tożsamość, Strategia strategia) {
		this.maksymalneŻycie = (int) (Math.random() * ((4 - 3) + 1) + 3);
		this.tożsamość = tożsamość;
		this.strategia = strategia;
		this.widokGracza = new WidokGracza(this);
		this.strategia.ustawGracza(this.widokGracza);
		this.karty = new ArrayList<>();
	}

	/**
	 * Konstruktor Gracza.
	 * @param tożsamość tożsamość gracza
	 * @param strategia strategia gracza
	 * @param maksymalneŻycie maksymalne życie gracza
	 */
	Gracz(Tożsamość tożsamość, Strategia strategia, int maksymalneŻycie) {
		this.maksymalneŻycie = maksymalneŻycie;
		this.tożsamość = tożsamość;
		this.strategia = strategia;
		this.widokGracza = new WidokGracza(this);
		this.strategia.ustawGracza(this.widokGracza);
		this.karty = new ArrayList<>();
	}

	/**
	 * Funkcja wywoływana na początku nowej gry.
	 * @param gra gra
	 * @param stół stół
	 * @param numerPrzyStole numer przy stole
	 */
	void zagrajWGrę(Gra gra, WidokStołu stół, Integer numerPrzyStole) {
		this.gra = gra;
		this.numerPrzyStole = numerPrzyStole;
		this.życie = this.maksymalneŻycie;
		this.zasięg = 1;
		this.stół = stół;
		this.karty.clear();
		this.strategia.ustawStół(stół);
	}

	/**
	 * Funkcja zwraca zasięg gracza.
	 * @return zasięg gracza
	 */
	int zasięg() {
		return this.zasięg;
	}

	/**
	 * Funkcja zwraca listę z kartami gracza.
	 * @return kopia listy kart
	 */
	List<Karta> karty() {
		return new ArrayList<>(this.karty);
	}

	/**
	 * Funkcja zwraca numer gracza przy stole.
	 * @return numer gracza
	 */
	Integer numerPrzyStole() {
		return this.numerPrzyStole;
	}

	/**
	 * Funkcja zwraca tożsamość gracza.
	 * @return tożsamość gracza.
	 */
	Tożsamość tożsamość() {
		return this.tożsamość;
	}

	/**
	 * Funkcja sprawdza czy gracz żyje.
	 * @return czy gracz żyje?
	 */
	boolean czyŻyję() {
		return this.życie != 0;
	}

	/**
	 * Funkcja sprawdza czy gracz ma pełne życie.
	 * @return czy pełne życie?
	 */
	boolean czyPełneŻycie() {
		return this.życie() == this.pełneŻycie();
	}

	/**
	 * Funkcja zwraca aktualne życie gracza.
	 * @return życie gracza
	 */
	int życie() {
		return this.życie;
	}

	/**
	 * Funkcja zwraca maksymalne życie gracza.
	 * @return maksymalne życie gracza
	 */
	int pełneŻycie() {
		return this.maksymalneŻycie;
	}

	/**
	 * Wykonaj akcję STRZEL.
	 * @param cel gracz, w którego strzela
	 * @throws KoniecGry gdy zostaną spełnione warunki zwycięstwa
	 */
	void akcjaStrzel(Integer cel) throws KoniecGry {
		Karta kartaStrzel = null;

		for(Karta karta : this.karty) {
			if(karta.akcja() == Akcja.STRZEL) {
				kartaStrzel = karta;
			}
		}

		if(kartaStrzel != null) {
			this.karty.remove(kartaStrzel);
			this.gra.strzel(kartaStrzel, cel);
		}
	}

	/**
	 * Wykonaj akcję ULECZ.
	 * @param cel gracz, którego uleczyć
	 */
	void akcjaUlecz(Integer cel) {
		Karta kartaUlecz = null;

		for(Karta karta : this.karty) {
			if(karta.akcja() == Akcja.ULECZ) {
				kartaUlecz = karta;
			}
		}

		if(kartaUlecz != null
				&& (this.stół.graczNaLewo().equals(cel)
					|| this.numerPrzyStole.equals(cel)
					|| this.stół.graczNaPrawo().equals(cel)
					)
			) {
			this.karty.remove(kartaUlecz);
			this.gra.uleczŻycie(kartaUlecz, cel);
		}
	}

	/**
	 * Wykonaj akcję ULECZ.
	 */
	void akcjaUlecz() {
		this.akcjaUlecz(this.numerPrzyStole);
	}

	/**
	 * Wykonaj akcję ZASIEG_PLUS_JEDEN.
	 */
	void akcjaZasięgPlusJeden() {
		Karta kartaZasięg = null;

		for(Karta karta : this.karty) {
			if(karta.akcja() == Akcja.ZASIEG_PLUS_JEDEN) {
				kartaZasięg = karta;
			}
		}

		if(kartaZasięg != null) {
			this.karty.remove(kartaZasięg);
			this.gra.dodajZasięg(kartaZasięg);
		}
	}

	/**
	 * Wykonaj akcję ZASIEG_PLUS_DWA.
	 */
	void akcjaZasięgPlusDwa() {
		Karta kartaZasięg = null;

		for(Karta karta : this.karty) {
			if(karta.akcja() == Akcja.ZASIEG_PLUS_DWA) {
				kartaZasięg = karta;
			}
		}

		if(kartaZasięg != null) {
			this.karty.remove(kartaZasięg);
			this.gra.dodajZasięg(kartaZasięg);
		}
	}

	/**
	 * Wykonaj akcję DYNAMIT.
	 */
	void akcjaDynamit() {
		Karta kartaDynamit = null;

		for(Karta karta : this.karty) {
			if(karta.akcja() == Akcja.DYNAMIT) {
				kartaDynamit = karta;
			}
		}

		if(kartaDynamit != null) {
			this.karty.remove(kartaDynamit);
			this.gra.rzućDynamit(kartaDynamit);
		}
	}

	/**
	 * Wykonaj turę gracza.
	 * @throws KoniecGry gdy zostaną spełnione warunki zwycięstwa
	 */
	void wykonajSwojąTurę() throws KoniecGry {
		this.strategia.wykonajSwojąTurę();
	}

	/**
	 * Zwiększ życie gracza.
	 * @param życie liczbą punktów życia
	 */
	void dodajŻycie(int życie) {
		this.życie = Math.min(this.życie + Math.abs(życie), this.maksymalneŻycie);
	}

	/**
	 * Zmniejsz życie gracza.
	 * @param życie liczba punktów życia
	 */
	void odejmijŻycie(int życie) {
		this.życie = Math.max(this.życie - Math.abs(życie), 0);
	}

	/**
	 * Zwiększ zasięg gracza.
	 * @param zasięg o ile zwiększyć zasięg
	 */
	void dodajZasięg(int zasięg) {
		this.zasięg += zasięg;
	}

	/**
	 * Dobierz brakujące karty na rękę gracza.
	 */
	void dobierzKarty() {
		if(this.karty.size() < 5) {
			this.karty.addAll(this.gra.dajKarty(5 - this.karty.size()));
		}
	}

	/**
	 * Zwróć wszystkie karty.
	 * @return lista kart
	 */
	List<Karta> oddajWszystkieKarty() {
		List<Karta> karty = new ArrayList<>(this.karty);
		this.karty.clear();

		return karty;
	}
}
