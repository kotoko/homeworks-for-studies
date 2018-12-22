package dzikizachod;

import java.util.ArrayList;
import java.util.List;

/**
 * Strategia szeryfa zliczającego.
 */
public class StrategiaSzeryfaZliczajaca extends StrategiaSzeryfa {
	/**
	 * Funkcja sprawdza czy gracz jest podejrzany o bycie bandytą.
	 * Jeśli strzelał do szeryfa lub zabiłe więcej pomocników niż bandytów zwraca true.
	 * @param podejrzany numer gracza
	 * @return czy podejrzany?
	 */
	private boolean czyPodejrzanyOBycieBandytą(Integer podejrzany) {
		for(Integer gracz : this.stół.ktoZaatakowałGracza(this.stół.numerSzeryfa())) {
			if(podejrzany.equals(gracz)) {
				return true;
			}
		}

		int bandyci = 0;
		int pomocnicy = 0;

		for(Integer gracz : this.stół.zabiciPrzezGracza(podejrzany)) {
			if(this.stół.tożsamośćGracza(gracz) == Tożsamość.BANDYTA) {
				bandyci++;
			}
			if(this.stół.tożsamośćGracza(gracz) == Tożsamość.POMOCNIK_SZERYFA) {
				pomocnicy++;
			}
		}

		return pomocnicy > bandyci;
	}

	/**
	 * Funkcja zwraca kolejny cel.
	 * Jeśli nie ma kolejnego celu zwraca -1.
	 * @return numer gracza
	 */
	private Integer kolejnyCel() {
		List<Integer> cele = new ArrayList<>();

		for(Integer gracz : this.stół.graczeWZasięgu()) {
			if(this.czyPodejrzanyOBycieBandytą(gracz)) {
				cele.add(gracz);
			}
		}

		if(!cele.isEmpty()) {
			return cele.get(this.losuj(cele.size()));
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
