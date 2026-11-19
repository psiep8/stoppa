package it.stoppa.backend.game;

import java.util.Objects;

/**
 * Una carta del mazzo napoletano.
 */
public record Carta(Seme seme, Valore valore) {

	public Carta {
		Objects.requireNonNull(seme, "seme");
		Objects.requireNonNull(valore, "valore");
	}

	public int punti() {
		return valore.punti();
	}

	@Override
	public String toString() {
		return valore.name() + " di " + seme.etichetta();
	}

}
