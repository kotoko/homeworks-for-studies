package mixingmachine.machine;

/**
 * Created by kuba on 09.06.17.
 */
public abstract class QualityChanger {
	protected final double value;
	private final QualityChangerType type;

	public QualityChanger(QualityChangerType type, double value) {
		this.type = type;
		this.value = value;
	}

	public QualityChangerType type(){
		return type;
	}

	public abstract int change(int quality);

	public double value(){
		return value;
	}
}
