package it.stoppa.backend.game;

import java.util.List;

/**
 * Le carte distribuite ai giocatori in una singola mano (massimo tre a testa).
 */
public record ManoDistribuita(List<List<Carta>> cartePerGiocatore) {

	public ManoDistribuita {
		cartePerGiocatore = cartePerGiocatore.stream().map(List::copyOf).toList();
	}

	public List<Carta> di(int giocatore) {
		return cartePerGiocatore.get(giocatore);
	}

	public int giocatori() {
		return cartePerGiocatore.size();
	}

	public int carteATesta() {
		return cartePerGiocatore.isEmpty() ? 0 : cartePerGiocatore.getFirst().size();
	}

}
