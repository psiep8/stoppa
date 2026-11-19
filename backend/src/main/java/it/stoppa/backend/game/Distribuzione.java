package it.stoppa.backend.game;

import java.util.List;

/**
 * L'intera distribuzione di una partita: le mani in ordine e le carte scartate
 * perche' il mazzo non si divideva per il numero di giocatori.
 */
public record Distribuzione(List<ManoDistribuita> mani, List<Carta> scartate) {

	public Distribuzione {
		mani = List.copyOf(mani);
		scartate = List.copyOf(scartate);
	}

	public ManoDistribuita mano(int indice) {
		return mani.get(indice);
	}

	public int numeroMani() {
		return mani.size();
	}

}
