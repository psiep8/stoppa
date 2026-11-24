package it.stoppa.backend.game;

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
		return carteDelPunto(carte).stream().mapToInt(Carta::punti).sum();
	}

	/**
	 * Le carte che fanno il punto, non tutta la mano: la combinazione migliore
	 * tra le carte piu' alte di un seme e la singola carta piu' alta. Se il punto
	 * lo fanno due carte, la terza resta fuori anche quando e' in mano.
	 */
	public static List<Carta> carteDelPunto(List<Carta> carte) {
		Objects.requireNonNull(carte, "carte");
		int migliore = 0;
		List<Carta> scelta = List.of();
		for (Seme seme : Seme.values()) {
			List<Carta> stessoSeme = carte.stream()
					.filter((carta) -> carta.seme() == seme)
					.sorted(Comparator.comparingInt(Carta::punti).reversed())
					.limit(MAX_CARTE_PER_SEME)
					.toList();
			int somma = stessoSeme.stream().mapToInt(Carta::punti).sum();
			if (somma > migliore) {
				migliore = somma;
				scelta = stessoSeme;
			}
		}
		// le carte restano nell'ordine della mano
		return scelta.isEmpty() ? List.of() : carte.stream().filter(scelta::contains).toList();
	}

}
