package mixingmachine.machine;

/**
 * Created by kuba on 09.06.17.
 */
public class QualityChangerMultiplier extends QualityChanger {
	public QualityChangerMultiplier(double value) {
		super(QualityChangerType.MULTIPLIER, value);
	}

	@Override
	public int change(int toxicity) {
		return (int) (((double) toxicity) * value);
	}

	@Override
	public String toString() {
		return "QualityChangerMultiplier{" +
				"value=" + value +
				'}';
	}
}
