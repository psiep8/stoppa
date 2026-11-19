package it.stoppa.backend.game;

/**
 * I quattro semi del mazzo napoletano.
 */
public enum Seme {

	DENARI("Denari"),
	COPPE("Coppe"),
	SPADE("Spade"),
	BASTONI("Bastoni");

	private final String etichetta;

	Seme(String etichetta) {
		this.etichetta = etichetta;
	}

	public String etichetta() {
		return etichetta;
	}

}
