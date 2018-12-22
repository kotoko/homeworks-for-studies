package mixingmachine.machine;

/**
 * Created by kuba on 09.06.17.
 */
public abstract class ToxicityChanger {
	protected final double value;
	private final ToxicityChangerType type;

	public ToxicityChanger(ToxicityChangerType toxicityChanger, double value) {
		this.type = toxicityChanger;
		this.value = value;
	}

	public ToxicityChangerType type(){
		return type;
	}

	public abstract int change(int toxicity);

	public double value(){
		return value;
	}
}
