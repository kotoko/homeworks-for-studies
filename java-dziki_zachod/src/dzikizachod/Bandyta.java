package dzikizachod;

/**
 * Klasa gracza - bandyty.
 */
public class Bandyta extends Gracz {
	/**
	 * Konstruktor bandyty ze strategią domyślną.
	 */
	public Bandyta() {
		super(Tożsamość.BANDYTA, new StrategiaBandytyDomyslna());
	}

	/**
	 * Konstruktor bandyty z zadaną strategią.
	 * @param strategia strategia bandyty
	 */
	public Bandyta(StrategiaBandyty strategia) {
		super(Tożsamość.BANDYTA, strategia);
	}
}
