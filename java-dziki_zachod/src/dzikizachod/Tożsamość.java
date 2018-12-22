package dzikizachod;

/**
 * Klasa tożssamości gracza.
 */
enum Tożsamość {
	NN,
	SZERYF,
	BANDYTA,
	POMOCNIK_SZERYFA;


	@Override
	public String toString() {
		switch(this) {
			case NN:
				return "N.N.";

			case SZERYF:
				return "Szeryf";

			case BANDYTA:
				return "Bandyta";

			case POMOCNIK_SZERYFA:
				return "Pomocnik Szeryfa";

			default:
				return super.toString();
		}
	}
}
