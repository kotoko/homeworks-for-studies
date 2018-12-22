package mixingmachine.machine;

import mixingmachine.parser.Content;
import mixingmachine.parser.InvalidConfigException;
import mixingmachine.parser.MyParser;
import mixingmachine.parser.Parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Symulacja maszyny mieszającej.
 */
public class MyMachine implements Machine {
	private List<Paint> paints = new ArrayList<>();
	private List<Pigment> pigments = new ArrayList<>();
	private boolean mixing = false;
	private Paint mixedPaint;

	@Override
	public void initilize(String configFile) {
		try {
			Parser parser = new MyParser();
			Content content = parser.parseConfig(configFile);

			this.paints = new ArrayList<>(content.paints());
			this.pigments = new ArrayList<>(content.pigments());
		}
		catch(InvalidConfigException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public Collection<Paint> paints() {
		return new ArrayList<>(paints);
	}

	@Override
	public Collection<Pigment> pigments() {
		return new ArrayList<>(pigments);
	}

	@Override
	public String paintName(String paintName) {
		for(Paint paint : paints) {
			if(paint.name().equals(paintName)) {
				return paint.name();
			}
		}
		return null;
	}

	@Override
	public int paintToxicity(String paintName) {
		for(Paint paint : paints) {
			if(paint.name().equals(paintName)) {
				return paint.toxicity();
			}
		}
		return -1;
	}

	@Override
	public void paintToxicity(String paintName, int toxicity) {
		for(Paint paint : paints) {
			if(paint.name().equals(paintName)) {
				paint.toxicity(toxicity);
				return;
			}
		}
	}

	@Override
	public int paintQuality(String paintName) {
		for(Paint paint : paints) {
			if(paint.name().equals(paintName)) {
				return paint.quality();
			}
		}
		return -1;
	}

	@Override
	public void paintQuality(String paintName, int quality) {
		for(Paint paint : paints) {
			if(paint.name().equals(paintName)) {
				paint.quality(quality);
				return;
			}
		}
	}

	@Override
	public String pigmentName(String pigmentName) {
		for(Pigment pigment : pigments) {
			if(pigment.name().equals(pigmentName)) {
				return pigment.name();
			}
		}
		return null;
	}

	@Override
	public String pigmentBeginPaint(String pigmentName) {
		for(Pigment pigment : pigments) {
			if(pigment.name().equals(pigmentName)) {
				return pigment.beginPaint();
			}
		}
		return null;
	}

	@Override
	public void pigmentBeginPaint(String pigmentName, String beginPaint) {
		for(Pigment pigment : pigments) {
			if(pigment.name().equals(pigmentName)) {
				pigment.beginPaint(beginPaint);
				return;
			}
		}
	}

	@Override
	public String pigmentEndPaint(String pigmentName) {
		for(Pigment pigment : pigments) {
			if(pigment.name().equals(pigmentName)) {
				return pigment.endPaint();
			}
		}
		return null;
	}

	@Override
	public void pigmentEndPaint(String pigmentName, String endPaint) {
		for(Pigment pigment : pigments) {
			if(pigment.name().equals(pigmentName)) {
				pigment.endPaint(endPaint);
				return;
			}
		}
	}

	@Override
	public ToxicityChangerType pigmentToxicityType(String pigmentName) {
		for(Pigment pigment : pigments) {
			if(pigment.name().equals(pigmentName)) {
				return pigment.toxicityChanger().type();
			}
		}
		return null;
	}

	@Override
	public void pigmentToxicityType(String pigmentName, ToxicityChangerType type) {
		for(Pigment pigment : pigments) {
			if(pigment.name().equals(pigmentName)) {
				switch(type){
					case MULTIPLIER:
						pigment.toxicityChanger(new ToxicityChangerMultiplier(pigment.toxicity()));
						break;

					case CONSTANT_PLUS:
						pigment.toxicityChanger(new ToxicityChangerConstant(pigment.toxicity()));
						break;

					case CONSTANT_MINUS:
						pigment.toxicityChanger(new ToxicityChangerConstant(((double)(-1))*pigment.toxicity()));
						break;

					default:
						pigment.toxicityChanger(null);
						break;
				}
				return;
			}
		}
	}

	@Override
	public double pigmentToxicity(String pigmentName) {
		for(Pigment pigment : pigments) {
			if(pigment.name().equals(pigmentName)) {
				return pigment.toxicity();
			}
		}
		return -1;
	}

	@Override
	public void pigmentToxicity(String pigmentName, double toxicity) {
		for(Pigment pigment : pigments) {
			if(pigment.name().equals(pigmentName)) {
				pigment.toxicity(toxicity);
				return;
			}
		}
	}

	@Override
	public QualityChangerType pigmentQualityType(String pigmentName) {
		for(Pigment pigment : pigments) {
			if(pigment.name().equals(pigmentName)) {
				return pigment.qualityChanger().type();
			}
		}
		return null;
	}

	@Override
	public void pigmentQualityType(String pigmentName, QualityChangerType type) {
		for(Pigment pigment : pigments) {
			if(pigment.name().equals(pigmentName)) {
				switch(type){
					case MULTIPLIER:
						pigment.qualityChanger(new QualityChangerMultiplier(pigment.quality()));
						break;

					case CONSTANT_PLUS:
						pigment.qualityChanger(new QualityChangerConstant(pigment.quality()));
						break;

					case CONSTANT_MINUS:
						pigment.qualityChanger(new QualityChangerConstant(((double)(-1))*pigment.quality()));
						break;

					default:
						pigment.qualityChanger(null);
						break;
				}
				return;
			}
		}
	}

	@Override
	public double pigmentQuality(String pigmentName) {
		for(Pigment pigment : pigments) {
			if(pigment.name().equals(pigmentName)) {
				return pigment.quality();
			}
		}
		return -1;
	}

	@Override
	public void pigmentQuality(String pigmentName, double quality) {
		for(Pigment pigment : pigments) {
			if(pigment.name().equals(pigmentName)) {
				pigment.quality(quality);
				return;
			}
		}
	}

	@Override
	public boolean containPaint(String paintName) {
		for(Paint paint : paints) {
			if(paint.name().equals(paintName)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean containPigment(String pigmentName) {
		for(Pigment pigment : pigments) {
			if(pigment.name().equals(pigmentName)) {
				return true;
			}
		}
		return false;
	}

	private String randomName() {
		return ("z" + UUID.randomUUID().toString().replaceAll("-", "")).substring(0,20);
	}

	@Override
	public String addRandomPaint() {
		String name;

		name = randomName();
		while(containPaint(name)) { //wierzę w rachunek prawdopodobieństwa :)
			name = randomName();
		}

		this.paints.add(new Paint(name, 0, 0));

		return name;
	}

	@Override
	public String addRandomPigment() {
		String name;

		name = randomName();
		while(containPigment(name)) { //wierzę w rachunek prawdopodobieństwa :)
			name = randomName();
		}

		this.pigments.add(new Pigment(name, randomName(), randomName(), (new QualityChangerMultiplier(1)), (new ToxicityChangerMultiplier(1))));

		return name;
	}

	@Override
	public void startxMixing(String paintName) {
		if(isMixing() || !containPaint(paintName)) {return;}

		mixing = true;

		for(Paint paint : paints) {
			if(paint.name().equals(paintName)) {
				mixedPaint = paint;
				break;
			}
		}

		System.out.println("Zaczynam mieszanie");
		System.out.println(mixedPaint.toStringPL());
	}

	@Override
	public void stopMixing() {
		if(!isMixing()) {return;}

		mixing = false;
		System.out.println("Koniec mieszania");
	}

	@Override
	public boolean isMixing() {
		return mixing;
	}

	@Override
	public boolean usePigment(String pigmentName) {
		if(!isMixing()){return false;}

		Pigment pigment = null;
		for(Pigment pigment1 : pigments) {
			if(pigment1.name().equals(pigmentName)){
				pigment = pigment1;
				break;
			}
		}

		if(pigment == null || !pigment.beginPaint().equals(mixedPaint.name())) {return false;}

		String name = pigment.endPaint();
		int toxicity = pigment.toxicityChanger().change(mixedPaint.toxicity());
		int quality = pigment.qualityChanger().change(mixedPaint.quality());

		if(!Paint.correct(name, toxicity, quality)){return false;}

		mixedPaint = new Paint(name, toxicity, quality);

		System.out.println(pigment.toStringPL());
		System.out.println(mixedPaint.toStringPL());

		return true;
	}
}
