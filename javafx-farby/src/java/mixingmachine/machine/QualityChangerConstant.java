package mixingmachine.machine;

/**
 * Created by kuba on 09.06.17.
 */
public class QualityChangerConstant extends QualityChanger {
	boolean minus = false;

	public QualityChangerConstant(double value) {
		super((value >= 0 ?
				QualityChangerType.CONSTANT_PLUS
				: QualityChangerType.CONSTANT_MINUS), Math.abs(value));
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
		return "QualityChangerConstant{" +
				"value=" + value +
				'}';
	}
}
