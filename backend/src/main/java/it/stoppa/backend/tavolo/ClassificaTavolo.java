package it.stoppa.backend.tavolo;

/** Una riga della classifica di fine partita: punto finale, gettoni, debito e cambi. */
public record ClassificaTavolo(int posizione, int indice, String nome, int punteggio, int fiches, int debito,
		int cambi) {
}
