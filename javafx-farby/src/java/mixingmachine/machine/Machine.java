package mixingmachine.machine;

import java.util.Collection;


/**
 * Interfejs maszyny mieszającej farby.
 */
public interface Machine {
	/**
	 * Funkcja służy do wczytania konfiguracji "maszyna.conf" do maszyny.
	 * @param configFile ścieżka do pliku konfiguracyjnego "maszyna.conf"
	 */
	void initilize(String configFile);

	/**
	 * Funkcja służy do poznania listy kolorów.
	 * @return kolekcja kolorów
	 */
	Collection<Paint> paints();

	/**
	 * Funkcja służy do poznania listy pigmentów.
	 * @return kolekcja pigmentów
	 */
	Collection<Pigment> pigments();

	/**
	 * Funkcja zwraca nazwę danej farby.
	 * Zwraca null jeśli nie ma w maszynie.
	 * @param paintName nazwa farby
	 * @return nazwa farby
	 */
	String paintName(String paintName);

	/**
	 * Funkcja zwraca toksyczność danej farby.
	 * Zwraca -1 jeśli nie ma w maszynie.
	 * @param paintName nazwa farby
	 * @return toksyczność
	 */
	int paintToxicity(String paintName);

	/**
	 * Funkcja zmienia toksyczność danej farby.
	 * Nic nie robi jeśli nie ma farby w maszynie.
	 * @param paintName nazwa farby
	 * @param toxicity nowa toksyczność
	 */
	void paintToxicity(String paintName, int toxicity);

	/**
	 * Funkcja zwraca jakość danej farby.
	 * Zwraca -1 jeśli nie ma w maszynie.
	 * @param paintName nazwa farby
	 * @return jakość
	 */
	int paintQuality(String paintName);

	/**
	 * Funkcja zmienia jakość danej farby.
	 * Nic nie robi jeśli nie ma farby w maszynie.
	 * @param paintName nazwa farby
	 * @param quality nowa jakość
	 */
	void paintQuality(String paintName, int quality);

	/**
	 * Funkcja zwraca nazwę danego pigmentu.
	 * Zwraca null jeśli nie pigmentu ma w maszynie.
	 * @param pigmentName nazwa farby
	 * @return nazwa farby
	 */
	String pigmentName(String pigmentName);

	/**
	 * Funkcja zwraca kolor początkowy danego pigmentu.
	 * Zwraca null jeśli nie ma w maszynie.
	 * @param pigmentName nazwa pigmentu
	 * @return kolor początkowy
	 */
	String pigmentBeginPaint(String pigmentName);

	/**
	 * Funkcja ustawia kolor początkowy danego pigmentu.
	 * Nic nie robi jeśli nie ma pigmentu w maszynie.
	 * @param pigmentName nazwa pigmentu
	 * @param beginPaint nowy kolor początkowy
	 */
	void pigmentBeginPaint(String pigmentName, String beginPaint);

	/**
	 * Funkcja zwraca końcowy kolor pigmentu.
	 * Zwraca null jeśli nie ma pigmentu w maszynie.
	 * @param pigmentName nazwa pigmentu
	 * @return kolor końcowy
	 */
	String pigmentEndPaint(String pigmentName);

	/**
	 * Funkcja ustawia kolor końcowy danego pigmentu.
	 * Nic nie robi jeśli nie ma pigmentu w maszynie.
	 * @param pigmentName nazwa pigmentu
	 * @param endPaint nowy kolor końcowy
	 */
	void pigmentEndPaint(String pigmentName, String endPaint);

	/**
	 * Funkcja zwraca typ „zmieniarki” toksyczności danego pigmentu.
	 * Zwraca null jeśli nie ma pigmentu w maszynie.
	 * @param pigmentName nazwa pigmentu
	 * @return typ „zmieniarki” toksyczności
	 */
	ToxicityChangerType pigmentToxicityType(String pigmentName);

	/**
	 * Funkcja ustawia typ „zmieniarki” toksyczności danego pigmentu.
	 * Nic nie robi jeśli nie ma pigmentu w maszynie.
	 * @param pigmentName nazwa pigmentu
	 * @param type nowy typ „zmieniarki” toksyczności
	 */
	void pigmentToxicityType(String pigmentName, ToxicityChangerType type);

	/**
	 * Funkcja zwraca toksyczność danego pigmentu.
	 * Zwraca null jeśli nie ma pigmentu w maszynie.
	 * @param pigmentName nazwa pigmentu
	 * @return toksyczność
	 */
	double pigmentToxicity(String pigmentName);

	/**
	 * Funkcja ustawia toksyczność danego pigmentu.
	 * Nic nie robi jeśli nie ma pigmentu w maszynie.
	 * @param pigmentName nazwa pigmentu
	 * @param toxicity nowa toksyczność
	 */
	void pigmentToxicity(String pigmentName, double toxicity);

	/**
	 * Funkcja zwraca typ „zmieniarki” jakości danego pigmentu.
	 * Zwraca null jeśli nie ma pigmentu w maszynie.
	 * @param pigmentName nazwa pigmentu
	 * @return typ „zmieniarki” jakości
	 */
	QualityChangerType pigmentQualityType(String pigmentName);

	/**
	 * Funkcja ustawia typ „zmieniarki” jakości danego pigmentu.
	 * Nic nie robi jeśli nie ma pigmentu w maszynie.
	 * @param pigmentName nazwa pigmentu
	 * @param type nowy typ „zmieniarki” jakości
	 */
	void pigmentQualityType(String pigmentName, QualityChangerType type);

	/**
	 * Funkcja zwraca jakość danego pigmentu.
	 * Zwraca -1 jeśli nie ma pigmentu w maszynie.
	 * @param pigmentName nazwa pigmentu
	 * @return jakość
	 */
	double pigmentQuality(String pigmentName);

	/**
	 * Funkcja ustawia jakość danego pigmentu.
	 * Nic nie robi jeśli nie ma pigmentu w maszynie.
	 * @param pigmentName nazwa pigmentu
	 * @param quality nowa jakość
	 */
	void pigmentQuality(String pigmentName, double quality);

	/**
	 * Funkcja sprawdza czy w maszynie jest dana farba.
	 * @param paintName nazwa farby
	 * @return czy w maszynie?
	 */
	boolean containPaint(String paintName);

	/**
	 * Funkcja sprawdza czy w maszynie jest dana farba.
	 * @param pigmentName nazwa pigmentu
	 * @return czy w maszynie?
	 */
	boolean containPigment(String pigmentName);

	/**
	 * Funkcja dodaje do maszyny nowy kolor o losowej nazwie.
	 * @return nazwa nowego koloru
	 */
	String addRandomPaint();

	/**
	 * Funkcja dodaje do maszyny nowy kolor o losowej nazwie.
	 * @return nazwa nowego koloru
	 */
	String addRandomPigment();

	/**
	 * Funkcja zaczyna mieszanie z zadaną farbą jako bazą początkową.
	 * Jeśli farby nie ma w maszynie to automatycznie kończy mieszanie.
	 * Jeśli funkcja jest wywołana gdy już rozpoczętą mieszanie nic się nie dzieje.
	 * @param paintName nazwa farby początkowej
	 */
	void startxMixing(String paintName);

	/**
	 * Funkcja kończy mieszanie.
	 * Jeśli funkcja jest wywołana gdy nie ma żadnego mieszania nic się nie dzieje.
	 */
	void stopMixing();

	/**
	 * Funkcja sprawdza czy obecnie trwa jakieś mieszanie.
	 * @return czy trwa mieszanie?
	 */
	boolean isMixing();

	/**
	 * Funkcja próbuje użyć pigmentu.
	 * Jeśli się uda to miesza.
	 * Jeśli się nie uda lub nie trwa żadne mieszanie nic się nie dzieje.
	 * @param pigmentName nazwa pigmentu
	 * @return czy się udało?
	 */
	boolean usePigment(String pigmentName);
}
