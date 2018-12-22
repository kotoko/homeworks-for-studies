package dzikizachod;

/**
 * Klasa ruchu wykonanego przez gracza.
 */
class Ruch {
	/** Gracz wykonujący ruch. */
	final Integer gracz;
	/** Akcja wykonana przez gracza. */
	final Akcja akcja;
	/** Gracz na którym akcja została wykonana. */
	final Integer cel;

	/**
	 * Konstruktor ruchu.
	 * @param gracz gracz wykonujący ruch
	 * @param akcja akcja wykonana
	 * @param cel gracz na którym akcja została wykonana
	 */
	Ruch(Integer gracz, Akcja akcja, Integer cel) {
		this.gracz = gracz;
		this.akcja = akcja;
		this.cel = cel;
	}

	@Override
	public String toString() {
		switch(this.akcja) {
			case STRZEL:
				return this.akcja.toString() + " " + this.cel;

			case ULECZ:
				if(gracz.equals(cel)) {
					return this.akcja.toString();
				}
				return this.akcja.toString() + " " + this.cel;

			default:
				return this.akcja.toString();
		}
	}
}
