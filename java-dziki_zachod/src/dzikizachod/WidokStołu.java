package dzikizachod;

import java.util.List;

/**
 * Klasa widoku stołu.
 * Komentarze funkcji - patrz klasa Stół.
 */
class WidokStołu {
	private final Stół stół;

	WidokStołu(Stół stół) {
		this.stół = stół;
	}

	int liczbaGraczy() {
		return this.stół.liczbaGraczy();
	}

	int liczbaŻywychGraczy() {
		return this.stół.liczbaŻywychGraczy();
	}

	int numerTury() {
		return this.stół.numerTury();
	}

	int limitTur() {
		return this.stół.limitTur();
	}

	Integer numerSzeryfa() {
		return this.stół.numerSzeryfa();
	}

	boolean czyGraczŻyje(Integer gracz) {
		return this.stół.czyGraczŻyje(gracz);
	}

	int życieGracza(Integer gracz) {
		return this.stół.życieGracza(gracz);
	}

	int pełneŻycieGracza(Integer gracz) {
		return this.stół.pełneŻycieGracza(gracz);
	}

	boolean czyPełneŻycieGracza(Integer gracz) {
		return this.stół.czyPełneŻycieGracza(gracz);
	}

	Tożsamość tożsamośćGracza(Integer gracz) {
		return this.stół.tożsamośćGracza(gracz);
	}

	Integer ktoZabiłGracza(Integer trup) {
		return this.stół.ktoZabiłGracza(trup);
	}

	List<Integer> ktoZaatakowałGracza(Integer ofiara) {
		return this.stół.ktoZaatakowałGracza(ofiara);
	}

	List<Integer> zabiciPrzezGracza(Integer gracz) {
		return this.stół.zabiciPrzezGracza(gracz);
	}

	List<Integer> strzałyGracza(Integer gracz) {
		return this.stół.strzałyGracza(gracz);
	}

	Integer graczNaPrawo(Integer gracz) {
		return this.stół.graczNaPrawo(gracz);
	}

	Integer graczNaPrawo() {
		return this.stół.graczNaPrawo();
	}

	Integer graczNaLewo(Integer gracz) {
		return this.stół.graczNaLewo(gracz);
	}

	Integer graczNaLewo() {
		return this.stół.graczNaLewo();
	}

	int odległośćMiędzyGraczamiWLewo(Integer odGracza, Integer doGracza) {
		return this.stół.odległośćMiędzyGraczamiWLewo(odGracza, doGracza);
	}

	int odległośćMiędzyGraczamiWPrawo(Integer odGracza, Integer doGracza) {
		return this.stół.odległośćMiędzyGraczamiWPrawo(odGracza, doGracza);
	}

	int odległośćMiędzyGraczami(Integer odGracza, Integer doGracza) {
		return this.stół.odległośćMiędzyGraczami(odGracza, doGracza);
	}

	int zasięgGracza(Integer gracz) {
		return this.stół.zasięgGracza(gracz);
	}

	boolean czyWZasięgu(Integer gracz, Integer cel) {
		return this.stół.czyWZasięgu(gracz, cel);
	}

	boolean czyWZasięgu(Integer cel) {
		return this.stół.czyWZasięgu(cel);
	}

	List<Integer> graczeWZasięgu(Integer gracz) {
		return this.stół.graczeWZasięgu(gracz);
	}

	List<Integer> graczeWZasięgu() {
		return this.stół.graczeWZasięgu();
	}

	List<Karta> kartyOdrzucone() {
		return this.stół.kartyOdrzucone();
	}

	List<Ruch> historiaRuchów() {
		return this.stół.historiaRuchów();
	}
}
