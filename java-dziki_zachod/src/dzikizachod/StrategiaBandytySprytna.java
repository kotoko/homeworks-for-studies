package dzikizachod;

import java.util.ArrayList;
import java.util.List;

/**
 * Klasa strategii bandyty sprytnego.
 */
public class StrategiaBandytySprytna extends StrategiaBandyty {
	/** Czy już zabił w tej turze innego bandytę? */
	private boolean zabiłSwojego;

	/**
	 * Funkcja zwraca kolejny cel.
	 * Jeśli nie ma kolejnego celu zwraca -1.
	 * @return numer gracza
	 */
	private Integer kolejnyCel() {
		if(this.stół.czyWZasięgu(this.stół.numerSzeryfa())) {
			return 1;
		}

		if(!zabiłSwojego) {
			List<Integer> bandyci = new ArrayList<>();

			for(Integer cel : this.bandyciWZasięgu()) {
				if(this.liczbaKartStrzel() >= this.stół.życieGracza(cel)) {
					bandyci.add(cel);
				}
			}

			if(!bandyci.isEmpty()) {
				return bandyci.get(this.losuj(bandyci.size()));
			}
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
		this.zabiłSwojego = false;

		while(this.liczbaKartStrzel() > 0 && cel > 0) {
			this.gracz.akcjaStrzel(cel);

			if(!this.stół.czyGraczŻyje(cel)) {
				if(this.stół.tożsamośćGracza(cel) == Tożsamość.BANDYTA) {
					this.zabiłSwojego = true;
				}

				cel = this.kolejnyCel();
			}
		}

		if(this.liczbaKartDynamit() > 0 && this.czyUżyćDynamitu()) {
			this.gracz.akcjaDynamit();
		}
	}
}
