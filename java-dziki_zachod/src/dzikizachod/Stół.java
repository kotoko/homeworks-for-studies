package dzikizachod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Klasa stołu.
 */
class Stół {
	/** Numer aktualnego gracza. */
	private Integer aktualnyGracz;
	/** Gra. */
	private Gra gra;
	/** Gracze. */
	private Gracz[] gracze;
	/** Liczba graczy na początku gry. */
	private int liczbaGraczy;
	/** Numer aktualnej tury. */
	private int numerTury;
	/** Limit tur. */
	private int limitTur;
	/** Historia ruchów w grze. */
	private List<Ruch> historiaRuchów;
	/** Lista martwych graczy. */
	private List<Integer> martwiGracze;
	/** Kto zabił danego gracza. */
	private Integer[] zabójcyGraczy;
	/** Lista osób zabitych przez gracza. */
	private List<List<Integer>> zabiciGracze;
	/** Lista wykonanych strzałów przez gracza. */
	private List<List<Integer>> wykonaneStrzały;
	/** Lista graczy, którzy strzelali do gracza. */
	private List<List<Integer>> strzelającyDoGracza;
	/** Karty odrzucone. */
	private PulaAkcji kartyOdrzucone;


	/**
	 * Kontruktor stołu.
	 * @param gra gra
	 */
	public Stół(Gra gra) {
		this.gra = gra;
		this.historiaRuchów = new ArrayList<>();
		this.martwiGracze = new ArrayList<>();
	}

	/**
	 * Funkcja dla numeru gracza zwraca indeks tego gracza w tablicy.
	 * @param gracz numer gracza
	 * @return indeks w tablicy
	 */
	private int gracz(Integer gracz) {
		return this.gra.gracz(gracz);
	}

	/**
	 * Dodaj informację o strzale.
	 * @param strzelec gracz strzelający
	 * @param cel gracz do którego strzela
	 */
	private void dodajStrzałDoGracza(Integer strzelec, Integer cel) {
		boolean duplikat = false;
		for(Integer strzelającyGracz : this.strzelającyDoGracza.get(this.gracz(cel))) {
			if(strzelającyGracz.equals(strzelec)) {
				duplikat = true;
			}
		}

		this.wykonaneStrzały.get(this.gracz(strzelec)).add(cel);

		if(!duplikat) {
			this.strzelającyDoGracza.get(this.gracz(cel)).add(strzelec);
		}
	}

	/**
	 * Funkcja usuwa z listy graczy graczy martwych.
	 * @param listaGraczy lista graczy
	 * @return lista graczy
	 */
	private List<Integer> usuńMartwychGraczy(List<Integer> listaGraczy) {
		return listaGraczy.stream().filter(gracz -> this.czyGraczŻyje(gracz)).collect(Collectors.toList());
	}


	/**
	 * Poinformuj stół o początku gry.
	 * @param gracze tablica graczy
	 * @param limitTur limit tur
	 * @param kartyOdrzucone pula kart odrzuconych
	 */
	void początekGry(Gracz gracze[], int limitTur, PulaAkcji kartyOdrzucone) {
		this.gracze = gracze;
		this.liczbaGraczy = gracze.length;
		this.numerTury = 0;
		this.limitTur = limitTur;
		this.historiaRuchów.clear();
		this.martwiGracze.clear();
		this.kartyOdrzucone = kartyOdrzucone;

		this.zabójcyGraczy = new Integer[this.liczbaGraczy];
		Arrays.fill(this.zabójcyGraczy, -1);

		this.zabiciGracze = new ArrayList<>(this.liczbaGraczy);
		this.wykonaneStrzały = new ArrayList<>(this.liczbaGraczy);
		this.strzelającyDoGracza = new ArrayList<>(this.liczbaGraczy);

		for(int i = 0; i < this.liczbaGraczy; ++i) {
			this.zabiciGracze.add(new ArrayList<>());
			this.wykonaneStrzały.add(new ArrayList<>());
			this.strzelającyDoGracza.add(new ArrayList<>());
		}
	}

	/**
	 * Poinformuj stół o śmierci gracza od dynamitu.
	 * @param trup numer gracza
	 */
	void umarłGraczOdDynamitu(Integer trup) {
		for(Integer martwyGracz : this.martwiGracze) {
			if(martwyGracz.equals(trup)) {
				return;
			}
		}

		this.martwiGracze.add(trup);
		this.zabójcyGraczy[this.gracz(trup)] = 0;
	}

	/**
	 * Poinformuj stół o śmierci gracza.
	 * @param zabójca numer gracza
	 * @param trup numer gracza
	 */
	void umarłGracz(Integer zabójca, Integer trup) {
		for(Integer martwyGracz : this.martwiGracze) {
			if(martwyGracz.equals(trup)) {
				return;
			}
		}

		this.martwiGracze.add(trup);
		this.zabójcyGraczy[this.gracz(trup)] = zabójca;
		this.zabiciGracze.get(this.gracz(zabójca)).add(trup);
	}

	/**
	 * Poinformuj stół o ruchu.
	 * @param ruch ruch
	 */
	void dodajRuch(Ruch ruch) {
		this.historiaRuchów.add(ruch);

		switch(ruch.akcja) {
			case STRZEL:
				this.dodajStrzałDoGracza(ruch.gracz, ruch.cel);
				break;
		}
	}

	/**
	 * Poinformuj stół o numerze tury.
	 * @param numerTury numer tury
	 */
	void ustawTurę(int numerTury) {
		this.numerTury = numerTury;
	}

	/**
	 * Poinformuj stół o aktualnym graczu.
	 * @param gracz numer gracza
	 */
	void ustawAktualnegoGracza(Integer gracz) {
		this.aktualnyGracz = gracz;
	}


	/**
	 * Funkcja zwraca liczbę graczy z początku gry.
	 * @return liczba graczy
	 */
	int liczbaGraczy() {
		return this.liczbaGraczy;
	}

	/**
	 * Funkcja zwraca liczbę żywych graczy.
	 * @return liczba graczy
	 */
	int liczbaŻywychGraczy() {
		return this.liczbaGraczy - this.martwiGracze.size();
	}

	/**
	 * Funkcja zwraca mumer tury.
	 * @return numer tury
	 */
	int numerTury() {
		return numerTury;
	}

	/**
	 * Funkcja zwraca limit tur.
	 * @return limit tur
	 */
	int limitTur() {
		return this.limitTur;
	}

	/**
	 * Funkcja zwraca numer szeryfa przy stole.
	 * @return numer szeryfa
	 */
	Integer numerSzeryfa() {
		return 1;
	}

	/**
	 * Funkcja sprawdza czy zadany gracz żyje.
	 * @param gracz numer gracza
	 * @return czy gracz żyje?
	 */
	boolean czyGraczŻyje(Integer gracz) {
		return this.gracze[this.gracz(gracz)].czyŻyję();
	}

	/**
	 * Funkcja zwraca aktualne życie gracza.
	 * @param gracz numer gracza
	 * @return życie gracza
	 */
	int życieGracza(Integer gracz) {
		return this.gracze[this.gracz(gracz)].życie();
	}

	/**
	 * Funkcja zwraca maksymalne życie gracza.
	 * @param gracz numer gracza
	 * @return maksymalne życie gracza
	 */
	int pełneŻycieGracza(Integer gracz) {
		return this.gracze[this.gracz(gracz)].pełneŻycie();
	}

	/**
	 * Funkcja sprawdza czy zadany gracz ma pełne życie.
	 * @param gracz numer gracza
	 * @return czy gracz ma pełne życie?
	 */
	boolean czyPełneŻycieGracza(Integer gracz) {
		return this.pełneŻycieGracza(gracz) == this.życieGracza(gracz);
	}

	/**
	 * Funkcja zwraca tożsamość zadanego gracza.
	 * Funkcja dba o prywatność i informuje o tożsamości tylko jeśli
	 * jest to zgodne z zasadami gry.
	 * @param gracz numer gracza
	 * @return tożsamość gracza
	 */
	Tożsamość tożsamośćGracza(Integer gracz) {
		Tożsamość tożsamość = this.gracze[this.gracz(gracz)].tożsamość();

		if(this.gracze[this.gracz(gracz)].czyŻyję()) {
			if(tożsamość == Tożsamość.POMOCNIK_SZERYFA) {
				tożsamość = Tożsamość.NN;
			}
			if(tożsamość == Tożsamość.BANDYTA
					&& this.gracze[this.gracz(this.aktualnyGracz)].tożsamość() != Tożsamość.BANDYTA) {
				tożsamość = Tożsamość.NN;
			}
		}

		return tożsamość;
	}

	/**
	 * Funkcja zwraca zabójcę danego gracza.
	 * Jeśli gracz żyje zwraca -1.
	 * Jeśli gracz został zabity przez dynamit zwraca 0.
	 * @param trup numer gracza
	 * @return zabójca gracza
	 */
	Integer ktoZabiłGracza(Integer trup) {
		return this.zabójcyGraczy[this.gracz(trup)];
	}

	/**
	 * Funkcja zwraca listę graczy, które strzeliły do zadanego gracza.
	 * @param ofiara numer gracza
	 * @return lista graczy
	 */
	List<Integer> ktoZaatakowałGracza(Integer ofiara) {
		return this.usuńMartwychGraczy(new ArrayList<>(this.strzelającyDoGracza.get(this.gracz(ofiara))));
	}

	/**
	 * Funkcja zwraca listę graczy, które zostały zabite przez zadanego gracza.
	 * @param gracz numer gracza
	 * @return lista graczy
	 */
	List<Integer> zabiciPrzezGracza(Integer gracz) {
		return new ArrayList<>(this.zabiciGracze.get(this.gracz(gracz)));
	}

	/**
	 * Funkcja zwraca listę strzałów zadanego gracza.
	 * Na liście mogą być duplikaty celów.
	 * Na liście będą tylko żywe cele.
	 * @param gracz numer gracza
	 * @return lista celów strzałów
	 */
	List<Integer> strzałyGracza(Integer gracz) {
		return this.usuńMartwychGraczy(new ArrayList<>(this.wykonaneStrzały.get(this.gracz(gracz))));
	}

	/**
	 * Funkcja zwraca numer gracza siedzącego na prawo od zadanego gracza.
	 * @param gracz numer gracza
	 * @return numer gracza na prawo
	 */
	Integer graczNaPrawo(Integer gracz) {
		if(gracz == 1) {
			return this.liczbaGraczy();
		}

		return gracz - 1;
	}

	/**
	 * Funkcja zwraca numer gracza siedzącego na prawo od aktulnego gracza.
	 * @return numer gracza na prawo
	 */
	Integer graczNaPrawo() {
		return graczNaPrawo(this.aktualnyGracz);
	}

	/**
	 * Funkcja zwraca numer gracza siedzącego na lewo od zadanego gracza.
	 * @param gracz numer gracza
	 * @return numer gracza na lewo
	 */
	Integer graczNaLewo(Integer gracz) {
		if(gracz.equals(this.liczbaGraczy())) {
			return 1;
		}

		return gracz + 1;
	}

	/**
	 * Funkcja zwraca numer gracza siedzącego na lewo od aktualnego gracza.
	 * @return numer gracza na lewo
	 */
	Integer graczNaLewo() {
		return graczNaLewo(this.aktualnyGracz);
	}

	/**
	 * Funkcja zwraca odległość między graczami w kierunku zgodnym
	 * z ruchem wskazówek zegara.
	 * Gracze martwi nie zwiększają odległości.
	 * @param odGracza numer gracza
	 * @param doGracza numer gracza
	 * @return odległość między graczami
	 */
	int odległośćMiędzyGraczamiWLewo(Integer odGracza, Integer doGracza) {
		Integer odległośćWLewo = 0;
		Integer gracz;

		gracz = odGracza;

		while(!gracz.equals(doGracza)) {
			gracz = this.graczNaLewo(gracz);

			if(this.czyGraczŻyje(gracz)) {
				odległośćWLewo++;
			}
		}

		return odległośćWLewo;
	}

	/**
	 * Funkcja zwraca odległość między graczami w kierunku przeciwnym
	 * do ruchem wskazówek zegara.
	 * Gracze martwi nie zwiększają odległości.
	 * @param odGracza numer gracza
	 * @param doGracza numer gracza
	 * @return odległość między graczami
	 */
	int odległośćMiędzyGraczamiWPrawo(Integer odGracza, Integer doGracza) {
		Integer odległośćWPrawo = 0;
		Integer gracz;

		gracz = odGracza;

		while(!gracz.equals(doGracza)) {
			gracz = this.graczNaPrawo(gracz);

			if(this.czyGraczŻyje(gracz)) {
				odległośćWPrawo++;
			}
		}

		return odległośćWPrawo;
	}

	/**
	 * Funkcja zwraca najkrótszą odległość między graczami.
	 * Gracze martwi nie zwiększają odległości.
	 * @param odGracza numer gracza
	 * @param doGracza numer gracza
	 * @return odległość między graczami
	 */
	int odległośćMiędzyGraczami(Integer odGracza, Integer doGracza) {
		return Math.min(
				this.odległośćMiędzyGraczamiWLewo(odGracza,doGracza),
				this.odległośćMiędzyGraczamiWPrawo(odGracza,doGracza)
		);
	}

	/**
	 * Funkcja zwraca zasięg zadanego gracza.
	 * @param gracz numer gracza
	 * @return zasięg gracza
	 */
	int zasięgGracza(Integer gracz) {
		return this.gracze[this.gracz(gracz)].zasięg();
	}

	/**
	 * Funkcja sprawdza czy zadany gracz jest w zasięgu zadanego gracza.
	 * @param gracz numer gracza
	 * @param cel numer gracza
	 * @return czy gracz w zasięgu?
	 */
	boolean czyWZasięgu(Integer gracz, Integer cel) {
		return this.odległośćMiędzyGraczami(gracz, cel) <= this.zasięgGracza(gracz);
	}

	/**
	 * Funkcja sprawdza czy zadany gracz jest w zasięgu aktualnego gracza.
	 * @param cel numer gracza
	 * @return czy gracz w zasięgu?
	 */
	boolean czyWZasięgu(Integer cel) {
		return this.czyWZasięgu(this.aktualnyGracz, cel);
	}

	/**
	 * Funkcja zwraca listę graczy w zasięgu zadanego gracza.
	 * Lista zawiera tylko graczy żywych.
	 * @param gracz numer gracza
	 * @return lista graczy
	 */
	List<Integer> graczeWZasięgu(Integer gracz) {
		List<Integer> listaGraczy = new ArrayList<>();

		for(Integer innyGracz = 1; innyGracz <= this.liczbaGraczy; ++innyGracz) {
			if(this.odległośćMiędzyGraczami(gracz, innyGracz) <= this.zasięgGracza(gracz)
					&& !innyGracz.equals(gracz)) {
				listaGraczy.add(innyGracz);
			}
		}

		return this.usuńMartwychGraczy(listaGraczy);
	}

	/**
	 * Funkcja zwraca listę graczy w zasięgu aktualnego gracza.
	 * Lista zawiera tylko graczy żywych.
	 * @return lista graczy
	 */
	List<Integer> graczeWZasięgu() {
		return this.graczeWZasięgu(this.aktualnyGracz);
	}

	/**
	 * Funkcja zwraca karty odrzucone.
	 * @return lista kart
	 */
	List<Karta> kartyOdrzucone() {
		return this.kartyOdrzucone.obejrzyjWszystkie();
	}

	/**
	 * Funkcja zwraca historię ruchów w grze.
	 * @return lista ruchów
	 */
	List<Ruch> historiaRuchów() {
		return new ArrayList<>(this.historiaRuchów);
	}
}
