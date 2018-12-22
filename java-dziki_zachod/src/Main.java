import dzikizachod.*;

import java.util.ArrayList;
import java.util.List;

public class Main {

	public static void main(String[] args) {
		List<Gracz> gracze = new ArrayList<>();

		gracze.add(new Szeryf(new StrategiaSzeryfaZliczajaca()));
		for(int i=0;i<2;i++)
			gracze.add(new PomocnikSzeryfa());
		for(int i=0;i<3;i++)
			gracze.add(new PomocnikSzeryfa(new StrategiaPomocnikaSzeryfaZliczajaca()));
		for(int i=0;i<3;i++)
			gracze.add(new Bandyta());
		for(int i=0;i<1;i++)
			gracze.add(new Bandyta(new StrategiaBandytySprytna()));
		for(int i=0;i<1;i++)
			gracze.add(new Bandyta(new StrategiaBandytyCierpliwa()));

		PulaAkcji pulaAkcji = new PulaAkcji();
		pulaAkcji.dodaj(Akcja.ULECZ, 40);
		pulaAkcji.dodaj(Akcja.STRZEL, 160);
		pulaAkcji.dodaj(Akcja.ZASIEG_PLUS_JEDEN, 3);
		pulaAkcji.dodaj(Akcja.ZASIEG_PLUS_DWA, 1);
		pulaAkcji.dodaj(Akcja.DYNAMIT, 1);

		Gra gra = new Gra();
		gra.rozgrywka(gracze, pulaAkcji);
	}
}
