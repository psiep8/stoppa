package it.stoppa.backend.game;

import java.util.List;

/**
 * Fotografia del tavolo per chi crea la lobby: gettoni, debiti e punto di ogni
 * giocatore, piatto e mano in corso.
 */
public record RiepilogoPartita(int piatto, int piattoIniziale, int numeroMano, int maniTotali, String cartaro,
		boolean finita, List<Integer> classifica, List<RigaGiocatore> giocatori) {

	public record RigaGiocatore(int indice, String nome, int fiches, int debito, int punteggio, boolean inGioco,
			int puntata) {
	}

}
