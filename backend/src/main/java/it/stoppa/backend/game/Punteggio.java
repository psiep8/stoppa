package it.stoppa.backend.game;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Punto di una mano: il migliore tra la somma delle carte piu' alte dello stesso
 * seme (al massimo tre) e la singola carta di valore piu' alto tra semi diversi.
 * Il massimo teorico e' 55, cioe' 7 + 6 + asso dello stesso seme.
 */
public final class Punteggio {

	public static final int MASSIMO = 55;

	public static final int MAX_CARTE_PER_SEME = 3;

	private Punteggio() {
	}

	public static int calcola(List<Carta> carte) {
		Objects.requireNonNull(carte, "carte");
		int migliore = 0;
		for (Seme seme : Seme.values()) {
			List<Integer> punti = new ArrayList<>();
			for (Carta carta : carte) {
				if (carta.seme() == seme) {
					punti.add(carta.punti());
				}
			}
			int somma = punti.stream()
					.sorted(Comparator.reverseOrder())
					.limit(MAX_CARTE_PER_SEME)
					.mapToInt(Integer::intValue)
					.sum();
			migliore = Math.max(migliore, somma);
		}
		return migliore;
	}

}
