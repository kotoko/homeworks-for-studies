package dzikizachod;

/**
 * Klasa wyniku gry.
 */
enum Wynik {
	REMIS,
	WYGRAL_SZERYF,
	WYGRALI_BANDYCI;


	@Override
	public String toString() {
		switch(this) {
			case REMIS:
				return "REMIS - OSIĄGNIĘTO LIMIT TUR";

			case WYGRAL_SZERYF:
				return "WYGRANA STRONA: szeryf i pomocnicy";

			case WYGRALI_BANDYCI:
				return "WYGRANA STRONA: bandyci";

			default:
				return super.toString();
		}
	}
}
