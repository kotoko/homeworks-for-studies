package dzikizachod;

/**
 * Klasa wyjątku KoniecGry używanej do zakończenia gry.
 */
class KoniecGry extends Exception {
	/** Wynik gry. */
	final Wynik wynik;

	/**
	 * Konstruktor wyjątku.
	 * @param wynik wynik gry
	 */
	KoniecGry(Wynik wynik) {
		this.wynik = wynik;
	}
}
