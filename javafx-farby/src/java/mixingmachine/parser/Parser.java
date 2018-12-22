package mixingmachine.parser;

/**
 * Interfejs parsera plików konfiguracyjnych.
 */
public interface Parser {
	/**
	 * Funkcja parsuje pliki konfiguracyjne.
	 * @param configFile ścieżka do pliku "maszyna.conf"
	 * @return kolekcje farb i pigmentów
	 * @throws InvalidConfigException parsowanie się nie powiodło
	 */
	Content parseConfig(String configFile) throws InvalidConfigException;
}
