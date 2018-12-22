package dzikizachod;

import java.util.ArrayList;
import java.util.List;

/**
 * Strategia szeryfa „domyślnego”.
 */
public class StrategiaSzeryfaDomyslna extends StrategiaSzeryfa {
	/**
	 * Funkcja zwraca kolejny cel.
	 * Jeśli nie ma kolejnego celu zwraca -1.
	 * @return numer gracza
	 */
	private Integer kolejnyCel() {
		List<Integer> graczeWZasięgu = this.stół.graczeWZasięgu();
		List<Integer> graczeAtakującySzeryfa = new ArrayList<>();

		for(Integer gracz : this.stół.ktoZaatakowałGracza(this.gracz.numerPrzyStole())) {
			if(this.stół.czyWZasięgu(gracz)) {
				graczeAtakującySzeryfa.add(gracz);
			}
		}

		if(!graczeAtakującySzeryfa.isEmpty()) {
			return graczeAtakującySzeryfa.get(this.losuj(graczeAtakującySzeryfa.size()));
		}

		if(!graczeWZasięgu.isEmpty()) {
			return graczeWZasięgu.get(this.losuj(graczeWZasięgu.size()));
		}

		return -1;
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
