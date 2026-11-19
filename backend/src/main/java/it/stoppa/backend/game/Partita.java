package it.stoppa.backend.game;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Una partita di stoppa: distribuisce le mani, gestisce i turni di puntata,
 * il piatto del tavolo e i cambi di gettoni.
 *
 * Il piatto si riempie una sola volta, un gettone a testa a inizio partita.
 * Ogni mano il vincitore prende le puntate e un gettone dal piatto; se nessuno
 * punta il gettone va al cartaro. A fine partita quello che resta nel piatto
 * va a chi ha il punto piu' alto.
 */
public final class Partita {

	public enum Fase {
		IN_CORSO, FINITA
	}

	private final List<Giocatore> giocatori;

	private final Distribuzione distribuzione;

	private final int piattoIniziale;

	private int piatto;

	private int cartaro;

	private int numeroMano = 1;

	private Mano mano;

	private Fase fase = Fase.IN_CORSO;

	public Partita(List<String> nomi, Random random) {
		Objects.requireNonNull(nomi, "nomi");
		Mazziere.validaGiocatori(nomi.size());
		this.giocatori = nomi.stream().map(Giocatore::new).toList();
		this.distribuzione = new Mazziere(random).distribuisci(nomi.size());
		this.piatto = nomi.size();
		this.piattoIniziale = piatto;
		this.mano = nuovaMano();
	}

	public List<Giocatore> giocatori() {
		return giocatori;
	}

	public Mano mano() {
		return mano;
	}

	public Distribuzione distribuzione() {
		return distribuzione;
	}

	public int piatto() {
		return piatto;
	}

	public int piattoIniziale() {
		return piattoIniziale;
	}

	public int cartaro() {
		return cartaro;
	}

	public int numeroMano() {
		return numeroMano;
	}

	public int maniTotali() {
		return distribuzione.numeroMani();
	}

	public Fase fase() {
		return fase;
	}

	public boolean finita() {
		return fase == Fase.FINITA;
	}

	public void passa() {
		mano.passa();
		dopoAzione();
	}

	public void vedo() {
		mano.vedo();
		dopoAzione();
	}

	public void punta(int gettoni) {
		mano.punta(gettoni);
		dopoAzione();
	}

	public void prossimaMano() {
		if (fase == Fase.FINITA) {
			throw new IllegalStateException("la partita e' finita");
		}
		if (!mano.chiusa()) {
			throw new IllegalStateException("la mano " + numeroMano + " e' ancora aperta");
		}
		if (numeroMano >= maniTotali()) {
			finalizza();
			return;
		}
		numeroMano++;
		cartaro = (cartaro + 1) % giocatori.size();
		mano = nuovaMano();
	}

	/** Classifica per punto finale: chi ha piu' punti, a parita' il primo in ordine di tavolo. */
	public List<Integer> classifica() {
		List<Integer> indici = new ArrayList<>();
		for (int i = 0; i < giocatori.size(); i++) {
			indici.add(i);
		}
		indici.sort(Comparator.comparingInt((Integer i) -> giocatori.get(i).punteggio()).reversed()
				.thenComparingInt(Integer::intValue));
		return List.copyOf(indici);
	}

	public RiepilogoPartita riepilogo() {
		List<RiepilogoPartita.RigaGiocatore> righe = new ArrayList<>();
		for (int i = 0; i < giocatori.size(); i++) {
			Giocatore giocatore = giocatori.get(i);
			righe.add(new RiepilogoPartita.RigaGiocatore(i, giocatore.nome(), giocatore.fiches(), giocatore.debito(),
					giocatore.punteggio(), mano.attivi().contains(i), mano.puntate().getOrDefault(i, 0)));
		}
		return new RiepilogoPartita(piatto, piattoIniziale, numeroMano, maniTotali(), giocatori.get(cartaro).nome(),
				finita(), finita() ? classifica() : List.of(), righe);
	}

	private void dopoAzione() {
		if (mano.chiusa()) {
			chiudi();
		}
	}

	private void chiudi() {
		for (int giocatore = 0; giocatore < giocatori.size(); giocatore++) {
			giocatori.get(giocatore).raccogli(mano.carteDi(giocatore));
		}
		if (mano.senzaPuntate()) {
			pagaDalPiatto(cartaro);
		}
		else {
			int vincitore = mano.vincitori().getFirst();
			giocatori.get(vincitore).aggiungiFiches(mano.totalePuntate());
			pagaDalPiatto(vincitore);
		}
		giocatori.forEach(Giocatore::cambio);
	}

	private void pagaDalPiatto(int giocatore) {
		if (piatto <= 0) {
			return;
		}
		piatto--;
		giocatori.get(giocatore).aggiungiFiches(1);
	}

	private Mano nuovaMano() {
		ManoDistribuita distribuita = distribuzione.mano(numeroMano - 1);
		List<List<Carta>> carte = new ArrayList<>(giocatori.size());
		for (int i = 0; i < giocatori.size(); i++) {
			carte.add(distribuita.di(i));
		}
		return new Mano(numeroMano, cartaro, giocatori, carte);
	}

	private void finalizza() {
		fase = Fase.FINITA;
		if (piatto > 0) {
			int vincitore = classifica().getFirst();
			giocatori.get(vincitore).aggiungiFiches(piatto);
			piatto = 0;
		}
	}

}
