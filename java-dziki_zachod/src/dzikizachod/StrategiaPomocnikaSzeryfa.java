package dzikizachod;

/**
 * Strategia pomocnika szeryfa.
 */
public abstract class StrategiaPomocnikaSzeryfa extends Strategia {
	/**
	 * Funkcja sprawdza czy gracz jest podejrzany o bycie bandytą.
	 * Jeśli strzelał do szeryfa lub zabiłe więcej pomocników niż bandytów zwraca true.
	 * @param podejrzany numer gracza
	 * @return czy podejrzany?
	 */
	boolean czyPodejrzanyOBycieBandytą(Integer podejrzany) {
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
	 * Funkcja decydująca czy użyć dynamitu.
	 * @return czy użyć dynamitu?
	 */
	boolean czyUżyćDynamitu() {
		if(this.stół.odległośćMiędzyGraczamiWLewo(
				this.gracz.numerPrzyStole(),
				this.stół.numerSzeryfa()
			) > 3) {

			int podejrzani = 0;
			Integer gracz;

			gracz = this.stół.graczNaLewo();
			while(!gracz.equals(this.stół.numerSzeryfa())) {
				if(this.stół.czyGraczŻyje(gracz)
						&& this.czyPodejrzanyOBycieBandytą(gracz)) {
					podejrzani++;
				}

				gracz = this.stół.graczNaLewo(gracz);
			}

			return 3 * podejrzani > 2 * (this.stół.odległośćMiędzyGraczamiWLewo(
					this.gracz.numerPrzyStole(),
					this.stół.numerSzeryfa()
				) - 1
			);
		}

		return false;
	}
}
