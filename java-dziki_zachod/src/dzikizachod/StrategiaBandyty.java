package dzikizachod;

import java.util.ArrayList;
import java.util.List;

/**
 * Klasa strategii bandyty.
 */
public abstract class StrategiaBandyty extends Strategia {
	/**
	 * Funkcja zwraca bandytów w zasięgu.
	 * @return lista numerów graczy
	 */
	List<Integer> bandyciWZasięgu() {
		List<Integer> bandyci = new ArrayList<>();

		for(Integer gracz : this.stół.graczeWZasięgu()) {
			if(this.stół.tożsamośćGracza(gracz) == Tożsamość.BANDYTA) {
				bandyci.add(gracz);
			}
		}

		return bandyci;
	}

	/**
	 * Funkcja zwraca pomocników szeryfa w zasięgu.
	 * @return lista numerów graczy
	 */
	List<Integer> pomocnicySzeryfaWZasięgu() {
		List<Integer> pomocnicy = new ArrayList<>();

		for(Integer gracz : this.stół.graczeWZasięgu()) {
			if(this.stół.tożsamośćGracza(gracz) == Tożsamość.NN) {
				pomocnicy.add(gracz);
			}
		}

		return  pomocnicy;
	}

	/**
	 * Funkcja decydująca czy użyć dynamitu.
	 * @return czy użyć dynamitu?
	 */
	boolean czyUżyćDynamitu() {
		return this.stół.odległośćMiędzyGraczamiWLewo(
				this.gracz.numerPrzyStole(),
				this.stół.numerSzeryfa()
		) < 3;
	}
}
