package mixingmachine.parser;

import mixingmachine.machine.*;

import java.io.*;
import java.util.*;

/**
 * Implementacja parsera.
 */
public class MyParser implements Parser {
	@Override
	public Content parseConfig(String configFile) throws InvalidConfigException {
		Config config;
		Collection<Paint> paints;
		Collection<Pigment> pigments;

		try {
			config = parseConfigMachine(configFile);
			paints = parseConfigPaints(config);
			pigments = parseConfigPigments(config);
		}
		catch(IOException cause) {
			throw new InvalidConfigIOException(cause, "Wystąpił błąd podczas wczytywania plików konfiguracji");
		}

		return new Content(paints, pigments);
	}

	private Config parseConfigMachine(String configFile) throws IOException, InvalidConfigContentException {
		File file= new File(configFile);
		File paintsFile;
		File pigmentsFile;

		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String currentLine;

			if((currentLine = reader.readLine()) == null) {
				throw new InvalidConfigContentException("Brak nazwy pliku z konfiguracją farb");
			}

			paintsFile = new File(file.getParentFile(), currentLine);

			if((currentLine = reader.readLine()) == null) {
				throw new InvalidConfigContentException("Brak nazwy pliku z konfiguracją pigmentów");
			}

			pigmentsFile = new File(file.getParentFile(), currentLine);

			if((currentLine = reader.readLine()) == null) {
				throw new InvalidConfigContentException("Brak nazwy sterownika");
			}

		}

		return new Config(paintsFile, pigmentsFile);
	}

	private Collection<Paint> parseConfigPaints(Config config) throws IOException, InvalidConfigContentException {
		List<Paint> paints = new ArrayList<>();
		File file = config.paintsPath;

		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String currentLine;

			while((currentLine = reader.readLine()) != null) {
				Scanner lineScanner = new Scanner(currentLine);
				String name;
				int toxicity;
				int quality;

				if(!lineScanner.hasNext()) {
					throw new InvalidConfigContentException("Brakuje nazwy farby");
				}

				name = lineScanner.next();

				if(!lineScanner.hasNextInt()) {
					throw new InvalidConfigContentException("Brakuje toksyczności farby");
				}

				toxicity = lineScanner.nextInt();

				if(!lineScanner.hasNextInt()) {
					throw new InvalidConfigContentException("Brakuje jakości farby");
				}

				quality = lineScanner.nextInt();

				if(!Paint.correct(name, toxicity, quality)) {
					throw new InvalidConfigContentException("Niepoprawne dane farby");
				}


				paints.add(new Paint(name, toxicity,quality));
			}
		}

		Map<String,Boolean> map = new HashMap<>();
		for(Paint paint : paints) {
			if(map.containsKey(paint.name())) {
				throw new InvalidConfigContentException("Farba o nazwie '" + paint.name() + "'wystąpiła więcej niż raz");
			}

			map.put(paint.name(), true);
		}

		return paints;
	}

	private Collection<Pigment> parseConfigPigments(Config config) throws IOException, InvalidConfigContentException {
		List<Pigment> pigments = new ArrayList<>();
		File file = config.pigmentsPath;


		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String currentLine;

			while((currentLine = reader.readLine()) != null) {
				Scanner lineScanner = new Scanner(currentLine);
				String name;
				String beginPaint;
				String endPaint;
				ToxicityChanger toxicityChanger;
				QualityChanger qualityChanger;

				if(!lineScanner.hasNext()) {
					throw new InvalidConfigContentException("Brakuje nazwy pigmentu");
				}

				name = lineScanner.next();

				if(!lineScanner.hasNext()) {
					throw new InvalidConfigContentException("Brakuje nazwy farby");
				}

				beginPaint = lineScanner.next();

				if(!lineScanner.hasNext()) {
					throw new InvalidConfigContentException("Brakuje nazwy farby");
				}

				endPaint = lineScanner.next();

				if(!lineScanner.hasNext()) {
					throw new InvalidConfigContentException("Brakuje parametru toksyczności");
				}

				{
					String token = lineScanner.next();
					String number = token.substring(1);
					double toxicity;

					if(number.length() == 0 || (token.charAt(0) != '-' && token.charAt(0) != '+' && token.charAt(0) != 'x')) {
						throw new InvalidConfigContentException("Niepoprawny parametr toksyczności");
					}

					Scanner sc = new Scanner(number);
					sc.useLocale(Locale.ENGLISH); // aby wczytywać ułamki z kropką, a nie przecinkiem

					if(!sc.hasNextDouble()) {
						throw new InvalidConfigContentException("Niepoprawny parametr toksyczności");
					}

					toxicity = sc.nextDouble();

					if(token.charAt(0) == '+') {
						toxicityChanger = new ToxicityChangerConstant(Math.abs(toxicity));
					}
					else if(token.charAt(0) == '-') {
						toxicityChanger = new ToxicityChangerConstant(((double)(-1)) * Math.abs(toxicity));
					}
					else if(token.charAt(0) == 'x') {
						toxicityChanger = new ToxicityChangerMultiplier(Math.abs(toxicity));
					}
					else {
						toxicityChanger = null;
					}
				}

				if(!lineScanner.hasNext()) {
					throw new InvalidConfigContentException("Brakuje parametru jakości");
				}

				{
					String token = lineScanner.next();
					String number = token.substring(1);
					double quality;

					if(number.length() == 0 || (token.charAt(0) != '-' && token.charAt(0) != '+' && token.charAt(0) != 'x')) {
						throw new InvalidConfigContentException("Niepoprawny parametr jakości");
					}

					Scanner sc = new Scanner(number);
					sc.useLocale(Locale.ENGLISH); // aby wczytywać ułamki z kropką, a nie przecinkiem

					if(!sc.hasNextDouble()) {
						throw new InvalidConfigContentException("Niepoprawny parametr jakości");
					}

					quality = sc.nextDouble();

					if(token.charAt(0) == '+') {
						qualityChanger = new QualityChangerConstant(Math.abs(quality));
					}
					else if(token.charAt(0) == '-') {
						qualityChanger = new QualityChangerConstant(((double)(-1)) * Math.abs(quality));
					}
					else if(token.charAt(0) == 'x') {
						qualityChanger = new QualityChangerMultiplier(Math.abs(quality));
					}
					else {
						qualityChanger = null;
					}
				}

				if(!Pigment.correct(name, beginPaint, endPaint, toxicityChanger.value(), qualityChanger.value())) {
					throw new InvalidConfigContentException("Niepoprawne dane pigmentu");
				}

				pigments.add(new Pigment(name, beginPaint, endPaint, qualityChanger, toxicityChanger));
			}
		}

		Map<String,Boolean> map = new HashMap<>();
		for(Pigment pigment : pigments) {
			if(map.containsKey(pigment.name())) {
				throw new InvalidConfigContentException("Pigment o nazwie '" + pigment.name() + "'wystąpił więcej niż raz");
			}

			map.put(pigment.name(), true);
		}

		return pigments;
	}
}
