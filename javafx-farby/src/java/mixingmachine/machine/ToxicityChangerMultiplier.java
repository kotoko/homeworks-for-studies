package mixingmachine.machine;

/**
 * Created by kuba on 09.06.17.
 */
public class ToxicityChangerMultiplier extends ToxicityChanger {
	public ToxicityChangerMultiplier(double value) {
		super(ToxicityChangerType.MULTIPLIER, value);
	}

	@Override
	public int change(int toxicity) {
		return (int) (((double) toxicity) * value);
	}

	@Override
	public String toString() {
		return "ToxicityChangerMultiplier{" +
				"value=" + value +
				'}';
	}
}
