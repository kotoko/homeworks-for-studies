package mixingmachine.machine;

/**
 * Created by kuba on 09.06.17.
 */
public class ToxicityChangerConstant extends ToxicityChanger {
	private boolean minus = false;

	public ToxicityChangerConstant(double value) {
		super((value >= 0 ?
						ToxicityChangerType.CONSTANT_PLUS
						: ToxicityChangerType.CONSTANT_MINUS),
				Math.abs(value)
		);
		if(value < 0) {
			minus = true;
		}
	}

	@Override
	public int change(int toxicity) {
		if(minus) {
			return (int) (((double) toxicity) - value);
		}
		return (int) (((double) toxicity) + value);
	}

	@Override
	public String toString() {
		return "ToxicityChangerConstant{" +
				"value=" + value +
				'}';
	}
}
