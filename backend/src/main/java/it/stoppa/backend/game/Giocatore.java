package it.stoppa.backend.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Un giocatore al tavolo: nome, gettoni e carte raccolte mano dopo mano.
 * Le fiches possono scendere sotto zero: si gioca a debito e l'automatico
 * cambio da dieci gettoni copre quello che manca a chi ha vinto.
 */
public final class Giocatore {

	public static final int FICHES_INIZIALI = 10;

	private final String nome;

	private int fiches;

	private final List<Carta> raccolte = new ArrayList<>();

	public Giocatore(String nome) {
		this.nome = Objects.requireNonNull(nome, "nome");
		this.fiches = FICHES_INIZIALI;
	}

	public String nome() {
		return nome;
	}

	public int fiches() {
		return fiches;
	}

	/** Gettoni giocati a debito e non ancora coperti dal cambio. */
	public int debito() {
		return Math.max(0, -fiches);
	}

	public void aggiungiFiches(int gettoni) {
		if (gettoni < 0) {
			throw new IllegalArgumentException("gettoni negativi: " + gettoni);
		}
		fiches += gettoni;
	}

	public void scalaFiches(int gettoni) {
		if (gettoni < 0) {
			throw new IllegalArgumentException("gettoni negativi: " + gettoni);
		}
		fiches -= gettoni;
	}

	/** Il cambio automatico: dieci gettoni nuovi quando si e' rimasti senza. */
	public boolean cambio() {
		if (fiches > 0) {
			return false;
		}
		fiches += FICHES_INIZIALI;
		return true;
	}

	public void raccogli(List<Carta> carte) {
		raccolte.addAll(carte);
	}

	public List<Carta> raccolte() {
		return List.copyOf(raccolte);
	}

	public int punteggio() {
		return Punteggio.calcola(raccolte);
	}

	@Override
	public String toString() {
		return nome + " (" + fiches + " gettoni)";
	}

}
