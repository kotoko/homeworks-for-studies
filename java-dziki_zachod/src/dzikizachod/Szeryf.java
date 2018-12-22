package dzikizachod;

/**
 * Klasa gracza - szeryfa.
 */
public class Szeryf extends Gracz {
	/**
	 * Konstruktor szeryfa z domyślną strategią.
	 */
	public Szeryf() {
		super(Tożsamość.SZERYF, new StrategiaSzeryfaDomyslna(), 5);
	}

	/**
	 * Konstruktor szeryfa z zadaną strategią.
	 * @param strategia strategia
	 */
	public Szeryf(StrategiaSzeryfa strategia) {
		super(Tożsamość.SZERYF, strategia, 5);
	}
}
