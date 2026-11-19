package it.stoppa.backend.game;

/**
 * Le dieci carte di ogni seme con il valore in punti della primiera:
 * le figure valgono 10, il 7 e' la carta piu' forte (21), l'asso 16.
 */
public enum Valore {

	ASSO(1, 16),
	DUE(2, 12),
	TRE(3, 13),
	QUATTRO(4, 14),
	CINQUE(5, 15),
	SEI(6, 18),
	SETTE(7, 21),
	FANTE(8, 10),
	CAVALLO(9, 10),
	RE(10, 10);

	private final int ordine;
	private final int punti;

	Valore(int ordine, int punti) {
		this.ordine = ordine;
		this.punti = punti;
	}

	/** Posizione nella scala del seme: asso 1, due 2, ... fante 8, cavallo 9, re 10. */
	public int ordine() {
		return ordine;
	}

	/** Punti della primiera. */
	public int punti() {
		return punti;
	}

	/** Fante, cavallo e re: sono le carte che si scartano quando il mazzo non si divide. */
	public boolean figura() {
		return ordine >= 8;
	}

}
