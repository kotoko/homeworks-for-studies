package dzikizachod;

import java.util.Random;

/**
 * Klasa strategii gracza.
 */
public abstract class Strategia {
	/** Gracz. */
	protected WidokGracza gracz;
	/** Generator liczb losowych. */
	private Random random;
	/** Stół. */
	protected WidokStołu stół;


	/**
	 * Funkcja wykonuje ruh gracza.
	 * @throws KoniecGry gdy zostaną spełnione warunki zwycięstwa
	 */
	abstract void wykonajSwojąTurę() throws KoniecGry;

	/**
	 * Funkcja informuje strategię o stole.
	 * @param stół widok stołu
	 */
	void ustawStół(WidokStołu stół) {
		this.stół = stół;
	}

	/**
	 * Funkcja informuje strategię o graczu.
	 * @param gracz widok gracza
	 */
	void ustawGracza(WidokGracza gracz) {
		this.gracz = gracz;
		this.random = new Random();
	}

	/**
	 * Funkcja losująca.
	 * Zwraca inta z przedziału [0;zakres).
	 * @param zakres zakres
	 * @return wylosowan liczba
	 */
	int losuj(int zakres) {
		return this.random.nextInt(zakres);
	}

	/**
	 * Funkcja zwraca liczbę kart STRZEL.
	 * @return liczba kart
	 */
	int liczbaKartStrzel() {
		int ile = 0;

		for(Karta karta : this.gracz.karty()) {
			if(karta.akcja() == Akcja.STRZEL) {
				ile++;
			}
		}

		return ile;
	}

	/**
	 * Funkcja zwraca liczbę kart ULECZ.
	 * @return liczba kart
	 */
	int liczbaKartUlecz() {
		int ile = 0;

		for(Karta karta : this.gracz.karty()) {
			if(karta.akcja() == Akcja.ULECZ) {
				ile++;
			}
		}

		return ile;
	}

	/**
	 * Funkcja zwraca liczbę kart ZASIĘG_PLUS_DWA.
	 * @return liczba kart
	 */
	int liczbaKartZasięgPlusDwa() {
		int ile = 0;

		for(Karta karta : this.gracz.karty()) {
			if(karta.akcja() == Akcja.ZASIEG_PLUS_DWA) {
				ile++;
			}
		}

		return ile;
	}

	/**
	 * Funkcja zwraca liczbę kart ZASIĘG_PLUS_JEDEN.
	 * @return liczba kart
	 */
	int liczbaKartZasięgPlusJeden() {
		int ile = 0;

		for(Karta karta : this.gracz.karty()) {
			if(karta.akcja() == Akcja.ZASIEG_PLUS_JEDEN) {
				ile++;
			}
		}

		return ile;
	}

	/**
	 * Funkcja zwraca liczbę kart DYNAMIT.
	 * @return liczba kart
	 */
	int liczbaKartDynamit() {
		int ile = 0;

		for(Karta karta : this.gracz.karty()) {
			if(karta.akcja() == Akcja.DYNAMIT) {
				ile++;
			}
		}

		return ile;
	}

}
