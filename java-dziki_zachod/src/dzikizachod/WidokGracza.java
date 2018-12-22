package dzikizachod;

import java.util.List;

/**
 * Klasa widoku gracza.
 * Komentarze funkcji - patrz klasa Gracz.
 */
class WidokGracza {
	private final Gracz gracz;

	WidokGracza(Gracz gracz) {
		this.gracz = gracz;
	}

	int zasięg() {
		return this.gracz.zasięg();
	}

	List<Karta> karty() {
		return this.gracz.karty();
	}

	Integer numerPrzyStole() {
		return this.gracz.numerPrzyStole();
	}

	Tożsamość tożsamość() {
		return this.gracz.tożsamość();
	}

	boolean czyŻyję() {
		return this.gracz.czyŻyję();
	}

	boolean czyPełneŻycie() {
		return this.gracz.czyPełneŻycie();
	}

	int życie() {
		return this.gracz.życie();
	}

	int pełneŻycie() {
		return this.gracz.pełneŻycie();
	}

	void akcjaStrzel(Integer cel) throws KoniecGry {
		this.gracz.akcjaStrzel(cel);
	}

	void akcjaUlecz(Integer cel) {
		this.gracz.akcjaUlecz(cel);
	}

	void akcjaUlecz() {
		this.gracz.akcjaUlecz();
	}

	void akcjaZasięgPlusJeden() {
		this.gracz.akcjaZasięgPlusJeden();
	}

	void akcjaZasięgPlusDwa() {
		this.gracz.akcjaZasięgPlusDwa();
	}

	void akcjaDynamit() {
		this.gracz.akcjaDynamit();
	}


}
