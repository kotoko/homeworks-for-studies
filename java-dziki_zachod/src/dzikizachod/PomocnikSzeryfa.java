package dzikizachod;

/**
 * Klasa gracza - pomocnika szeryfa.
 */
public class PomocnikSzeryfa extends Gracz {
	/**
	 * Konstruktor pomocnika szeryfa ze strategią domyślną.
	 */
	public PomocnikSzeryfa() {
		super(Tożsamość.POMOCNIK_SZERYFA, new StrategiaPomocnikaSzeryfaDomyslna());
	}

	/**
	 * Konstruktor pomocnika szeryfa z zadaną strategią.
	 * @param strategia strategia pomocnika szeryfa
	 */
	public PomocnikSzeryfa(StrategiaPomocnikaSzeryfa strategia) {
		super(Tożsamość.POMOCNIK_SZERYFA, strategia);
	}
}
