package mixingmachine.machine;

/**
 * Created by kuba on 10.06.17.
 */
public enum ToxicityChangerType {
	CONSTANT_PLUS,
	CONSTANT_MINUS,
	MULTIPLIER;

	@Override
	public String toString() {
		switch(this){
			case MULTIPLIER:
				return "x";

			case CONSTANT_PLUS:
				return "+";

			case CONSTANT_MINUS:
				return "-";

			default:
				return super.toString();
		}
	}
}
