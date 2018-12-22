package dzikizachod;

import java.util.List;

/**
 * Strategia pomocnika szeryfa „domyślnego”.
 */
public class StrategiaPomocnikaSzeryfaDomyslna extends StrategiaPomocnikaSzeryfa {
	/**
	 * Funkcja zwraca kolejny cel.
	 * Jeśli nie ma kolejnego celu zwraca -1.
	 * @return numer gracza
	 */
	private Integer kolejnyCel() {
		List<Integer> gracze = this.stół.graczeWZasięgu();
		Integer szeryf = null;

		for(Integer gracz : gracze) {
			if(gracz.equals(this.stół.numerSzeryfa())) {
				szeryf = gracz;
			}
		}

		if(szeryf != null) {
			gracze.remove(szeryf);
		}

		if(gracze.isEmpty()) {
			return -1;
		}

		return gracze.get(this.losuj(gracze.size()));
	}

	/**
	 * Funkcja sprawdza czy szeryf siedzi obok.
	 * @return czy szeryf siedzi obok?
	 */
	private boolean czySzeryfSiedziObok() {
		return this.stół.graczNaLewo().equals(this.stół.numerSzeryfa())
				|| this.stół.graczNaPrawo().equals(this.stół.numerSzeryfa());
	}

	@Override
	void wykonajSwojąTurę() throws KoniecGry {
		while(this.liczbaKartUlecz() > 0
				&& (!this.gracz.czyPełneŻycie() ||
					(!this.stół.czyPełneŻycieGracza(this.stół.numerSzeryfa()) && this.czySzeryfSiedziObok())
					)
				) {
			if(!this.stół.czyPełneŻycieGracza(this.stół.numerSzeryfa()) && this.czySzeryfSiedziObok()) {
				this.gracz.akcjaUlecz(this.stół.numerSzeryfa());
			}
			else {
				this.gracz.akcjaUlecz();
			}
		}

		while(this.liczbaKartZasięgPlusDwa() > 0) {
			this.gracz.akcjaZasięgPlusDwa();
		}

		while(this.liczbaKartZasięgPlusJeden() > 0) {
			this.gracz.akcjaZasięgPlusJeden();
		}

		Integer cel = this.kolejnyCel();
		while(this.liczbaKartStrzel() > 0 && cel > 0) {
			this.gracz.akcjaStrzel(cel);

			if(!this.stół.czyGraczŻyje(cel)) {
				cel = this.kolejnyCel();
			}
		}

		if(this.liczbaKartDynamit() > 0 && this.czyUżyćDynamitu()) {
			this.gracz.akcjaDynamit();
		}
	}
}
