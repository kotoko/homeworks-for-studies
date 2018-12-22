package dzikizachod;

/**
 * Klasa karty.
 */
class Karta {
	/** Akcja karty. */
	private final Akcja akcja;


	/**
	 * Konstruktor karty.
	 * @param akcja akcja karty
	 */
	Karta(Akcja akcja) {
		this.akcja = akcja;
	}

	/**
	 * Konstruktor karty.
	 * @param karta inna karta
	 */
	Karta(Karta karta) {
		this.akcja = karta.akcja;
	}

	/**
	 * Funkcja zwraca akcję karty.
	 * @return akcja karty.
	 */
	public Akcja akcja() {
		return this.akcja;
	}

	@Override
	public String toString() {
		return akcja.toString();
	}
}
