package dzikizachod;

import java.util.List;

/**
 * Klasa strategii bandyty „domyślnego”.
 */
public class StrategiaBandytyDomyslna extends StrategiaBandyty {
	/**
	 * Funkcja zwraca kolejny cel.
	 * Jeśli nie ma kolejnego celu zwraca -1.
	 * @return numer gracza
	 */
	private Integer kolejnyCel() {
		if(this.stół.czyWZasięgu(this.stół.numerSzeryfa())) {
			return this.stół.numerSzeryfa();
		}

		List<Integer> pomocnicy = this.pomocnicySzeryfaWZasięgu();

		if(pomocnicy.isEmpty()) {
			return -1;
		}

		return pomocnicy.get(this.losuj(pomocnicy.size()));
	}

	@Override
	void wykonajSwojąTurę() throws KoniecGry {
		while(this.liczbaKartUlecz() > 0 && !this.gracz.czyPełneŻycie()) {
			this.gracz.akcjaUlecz();
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
