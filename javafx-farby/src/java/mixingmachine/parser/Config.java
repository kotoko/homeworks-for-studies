package mixingmachine.parser;

import java.io.File;

/**
 * Klasa zawierająca pliki konfiguracyjne farb i pigmentów.
 */
class Config {
	public final File paintsPath;
	public final File pigmentsPath;

	public Config(File paintsPath, File pigmentsPath) {
		this.paintsPath = paintsPath;
		this.pigmentsPath = pigmentsPath;
	}

	@Override
	public String toString() {
		return "Config{" +
				"paintsPath='" + paintsPath + '\'' +
				", pigmentsPath='" + pigmentsPath + '\'' +
				'}';
	}
}
