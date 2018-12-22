package dzikizachod;

/**
 * Typ wyliczeniowy akcji.
 */
public enum Akcja {
	ULECZ,
	STRZEL,
	ZASIEG_PLUS_JEDEN,
	ZASIEG_PLUS_DWA,
	DYNAMIT;


	@Override
	public String toString() {
		switch(this){
			case ULECZ:
				return "ULECZ";

			case STRZEL:
				return "STRZEL";

			case ZASIEG_PLUS_JEDEN:
				return "ZASIEG_PLUS_JEDEN";

			case ZASIEG_PLUS_DWA:
				return "ZASIEG_PLUS_DWA";

			case DYNAMIT:
				return "DYNAMIT";

			default:
				return super.toString();
		}
	}
}
