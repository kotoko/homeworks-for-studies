package mixingmachine.machine;

import java.util.regex.Pattern;

/**
 * Klasa pigmentu.
 */
public class Pigment {
	private String name;
	private String beginPaint;
	private String endPaint;
	private QualityChanger qualityChanger;
	private ToxicityChanger toxicityChanger;

	public Pigment(String name, String beginPaint, String endPaint, QualityChanger qualityChanger, ToxicityChanger toxicityChanger) {
		this.name = name;
		this.beginPaint = beginPaint;
		this.endPaint = endPaint;
		this.qualityChanger = qualityChanger;
		this.toxicityChanger = toxicityChanger;
	}

	@Override
	public String toString() {
		return "Pigment{" +
				"name='" + name + '\'' +
				"beginPaint='" + beginPaint + '\'' +
				", endPaint='" + endPaint + '\'' +
				", qualityChanger=" + qualityChanger +
				", toxicityChanger=" + toxicityChanger +
				'}';
	}

	public String toStringPL() {
		return "Pigment{" +
				"nazwa='" + name + '\'' +
				", farba_poczatkowa='" + beginPaint + '\'' +
				", farba_końcowa='" + endPaint + '\'' +
				", typ_zmieniarki_toksyczności=" + toxicityChanger.type() +
				", toksyczność=" + toxicityChanger.value() +
				", typ_zmieniarki_jakości=" + qualityChanger.type() +
				", jakość=" + qualityChanger.value() +
				'}';
	}

	public static boolean correctName(String name) {
		Pattern p = Pattern.compile("[^ąćęłóśźżĄĆĘŁÓŚŹŻa-zA-Z0-9]");
		return name.length() != 0 && !p.matcher(name).find();
	}

	public static boolean correctToxicity(double toxicity) {
		return !(toxicity > 100 || toxicity < 0);
	}

	public static boolean correctQuality(double quality) {
		return !(quality > 100 || quality < 0);
	}

	public static boolean correct(String name, String beginPaint, String endPaint, double toxicity, double quality) {
		return correctName(name)
			&& Paint.correctName(beginPaint)
			&& Paint.correctName(endPaint)
			&& correctToxicity(toxicity)
			&& correctQuality(quality);
	}

	public String name() {
		return name;
	}

	public void name(String name) {
		this.name = name;
	}

	public String beginPaint() {
		return beginPaint;
	}

	public void beginPaint(String beginPaint) {
		this.beginPaint = beginPaint;
	}

	public String endPaint() {
		return endPaint;
	}

	public void endPaint(String endPaint) {
		this.endPaint = endPaint;
	}

	public QualityChanger qualityChanger() {
		return qualityChanger;
	}

	public void qualityChanger(QualityChanger qualityChanger) {
		this.qualityChanger = qualityChanger;
	}

	public ToxicityChanger toxicityChanger() {
		return toxicityChanger;
	}

	public void toxicityChanger(ToxicityChanger toxicityChanger) {
		this.toxicityChanger = toxicityChanger;
	}

	public double toxicity() {
		return this.toxicityChanger().value();
	}

	public void toxicity(double toxicity) {
		switch(toxicityChanger.type()) {
			case MULTIPLIER:
				this.toxicityChanger = new ToxicityChangerMultiplier(toxicity);
				break;

			case CONSTANT_PLUS:
				this.toxicityChanger = new ToxicityChangerConstant(toxicity);
				break;

			case CONSTANT_MINUS:
				this.toxicityChanger = new ToxicityChangerConstant(((double)(-1))*toxicity);
				break;
		}
	}

	public double quality() {
		return this.qualityChanger().value();
	}

	public void quality(double quality) {
		switch(qualityChanger.type()) {
			case MULTIPLIER:
				this.qualityChanger = new QualityChangerMultiplier(quality);
				break;

			case CONSTANT_PLUS:
				this.qualityChanger = new QualityChangerConstant(quality);
				break;

			case CONSTANT_MINUS:
				this.qualityChanger = new QualityChangerConstant(((double)(-1))*quality);
				break;
		}
	}
}
