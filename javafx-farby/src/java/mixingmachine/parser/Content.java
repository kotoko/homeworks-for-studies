package mixingmachine.parser;

import mixingmachine.machine.Paint;
import mixingmachine.machine.Pigment;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Klasa zawierająca kolekcję farb i pigmentów.
 */
public class Content {
	private Collection<Paint> paints;
	private Collection<Pigment> pigments;

	public Content(Collection<Paint> paints, Collection<Pigment> pigments) {
		this.paints = new ArrayList<>(paints);
		this.pigments = new ArrayList<>(pigments);
	}

	public Collection<Paint> paints() {
		return new ArrayList<>(paints);
	}

	public Collection<Pigment> pigments() {
		return new ArrayList<>(pigments);
	}
}
