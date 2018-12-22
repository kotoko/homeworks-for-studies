package dzikizachod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Klasa puli akcji.
 */
public class PulaAkcji {
	/** Zawartość puli akcji. */
	private final List<Karta> pula;


	/**
	 * Konstruktor puli akcji z zadanej puli akcji.
	 * @param pulaAkcji istniejąca pula akcji
	 */
	public PulaAkcji(PulaAkcji pulaAkcji) {
		this.pula = new ArrayList<>(pulaAkcji.pula);
	}

	/**
	 * Konstruktor pustej puli akcji.
	 */
	public PulaAkcji() {
		this.pula = new ArrayList<>();
	}

	/**
	 * Funkcja tworzy karty i dodaje je do puli akcji.
	 * @param akcja akcja karty
	 * @param liczba liczba kart
	 */
	public void dodaj(Akcja akcja, int liczba) {
		for(int i = 0; i < liczba; ++i) {
			switch(akcja){
				case ULECZ:
					this.pula.add(new Karta(Akcja.ULECZ));
					break;

				case STRZEL:
					this.pula.add(new Karta(Akcja.STRZEL));
					break;

				case DYNAMIT:
					this.pula.add(new Karta(Akcja.DYNAMIT));
					break;

				case ZASIEG_PLUS_JEDEN:
					this.pula.add(new Karta(Akcja.ZASIEG_PLUS_JEDEN));
					break;

				case ZASIEG_PLUS_DWA:
					this.pula.add(new Karta(Akcja.ZASIEG_PLUS_DWA));
					break;
			}
		}
	}

	/**
	 * Funkcja dodaje karty do puli.
	 * @param karty kolekcja kart
	 */
	void dodaj(Collection<Karta> karty) {
		this.pula.addAll(karty);
	}

	/**
	 * Funkcja dodaje kartę do puli.
	 * @param kart karta
	 */
	void dodaj(Karta kart) {
		this.pula.add(kart);
	}

	/**
	 * Funkcja tasuje zawartość puli akcji.
	 */
	void potasuj() {
		Collections.shuffle(this.pula);
	}

	/**
	 * Funkcja zwraca kartę z wierzchu.
	 * Karta jest usunięta z puli.
	 * @return karta
	 */
	Karta weźKartę() {
		Karta karta = this.pula.get(this.pula.size() - 1);
		this.pula.remove(this.pula.size() - 1);

		return karta;
	}

	/**
	 * Funkcja zwraca listę wszystkich kart z puli.
	 * Karty zostają usunięte z puli.
	 * @return lista kart
	 */
	List<Karta> weźWszystkie() {
		List<Karta> kopiaPuli = new ArrayList<>(this.pula);
		this.pula.clear();

		return kopiaPuli;
	}

	/**
	 * Funkcja zwraca listę wszystkich kart z puli.
	 * @return lista kart
	 */
	List<Karta> obejrzyjWszystkie() {
		return new ArrayList<>(this.pula);
	}

	/**
	 * Funkcja zwraca liczbę kart w puli.
	 * @return liczba kart
	 */
	int ileKart() {
		return this.pula.size();
	}
}
