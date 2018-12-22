package mixingmachine.machine;

import java.util.regex.Pattern;

/**
 * Klasa farby.
 */
public class Paint {
	private String name;
	private int toxicity;
	private int quality;

	public static boolean correctName(String name) {
		Pattern p = Pattern.compile("[^ąćęłóśźżĄĆĘŁÓŚŹŻa-zA-Z0-9-]");
		return !(name.length() == 0 || !Character.isLetter(name.charAt(0))) && !p.matcher(name).find();

	}

	public static boolean correctToxicity(int toxicity) {
		return !(toxicity > 100 || toxicity < 0);
	}

	public static boolean correctQuality(int quality) {
		return !(quality > 100 || quality < 0);
	}

	public static boolean correct(String name, int toxicity, int quality) {
		return correctName(name)
			&& correctToxicity(toxicity)
			&& correctQuality(quality);
	}

	public Paint(String name, int toxicity, int quality)  {
		this.name = name;
		this.toxicity = toxicity;
		this.quality = quality;
	}

	@Override
	public String toString() {
		return "Paint{" +
				"name='" + name + '\'' +
				", toxicity=" + toxicity +
				", quality=" + quality +
				'}';
	}

	public String toStringPL() {
		return "Farba{" +
				"nazwa='" + name + '\'' +
				", toksyczność=" + toxicity +
				", jakość=" + quality +
				'}';
	}

	public String name() {
		return name;
	}

	public void name(String name) {
		this.name = name;
	}

	public int toxicity() {
		return toxicity;
	}

	public void toxicity(int toxicity) {
		this.toxicity = toxicity;
	}

	public int quality() {
		return quality;
	}

	public void quality(int quality) {
		this.quality = quality;
	}
}
