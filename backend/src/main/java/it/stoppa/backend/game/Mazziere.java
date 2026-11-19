package it.stoppa.backend.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Il cartaro: prepara il mazzo, toglie le carte che avanzano e distribuisce le
 * mani. Con 40 carte e un numero di giocatori che non divide il mazzo si
 * scartano tante carte quante il resto, scegliendo le figure (le meno preziose).
 */
public final class Mazziere {

	public static final int CARTE_PER_MAZZO = 40;

	public static final int MIN_GIOCATORI = 4;

	public static final int MAX_GIOCATORI = 8;

	/** Nessuno puo' avere piu' di tre carte dello stesso seme in mano. */
	public static final int MAX_CARTE_PER_MANO = 3;

	private final Random random;

	public Mazziere(Random random) {
		this.random = Objects.requireNonNull(random, "random");
	}

	public static void validaGiocatori(int giocatori) {
		if (giocatori < MIN_GIOCATORI || giocatori > MAX_GIOCATORI) {
			throw new IllegalArgumentException(
					"si gioca da " + MIN_GIOCATORI + " a " + MAX_GIOCATORI + " giocatori, non " + giocatori);
		}
	}

	public static List<Carta> mazzoCompleto() {
		List<Carta> mazzo = new ArrayList<>(CARTE_PER_MAZZO);
		for (Seme seme : Seme.values()) {
			for (Valore valore : Valore.values()) {
				mazzo.add(new Carta(seme, valore));
			}
		}
		return mazzo;
	}

	public Distribuzione distribuisci(int giocatori) {
		validaGiocatori(giocatori);

		List<Carta> mazzo = mazzoCompleto();
		Collections.shuffle(mazzo, random);

		int resto = mazzo.size() % giocatori;
		List<Carta> scartate = scarta(mazzo, resto);

		int carteATesta = mazzo.size() / giocatori;
		List<Integer> tagli = tagli(carteATesta);

		List<ManoDistribuita> mani = new ArrayList<>(tagli.size());
		int cursore = 0;
		for (int dimensione : tagli) {
			List<List<Carta>> perGiocatore = new ArrayList<>(giocatori);
			for (int giocatore = 0; giocatore < giocatori; giocatore++) {
				perGiocatore.add(List.copyOf(mazzo.subList(cursore, cursore + dimensione)));
				cursore += dimensione;
			}
			mani.add(new ManoDistribuita(perGiocatore));
		}
		return new Distribuzione(mani, scartate);
	}

	/**
	 * Divide le carte di un giocatore in mani da al massimo tre: 10 diventa
	 * 3+3+3+1, 8 diventa 3+3+2, 6 diventa 3+3, 5 diventa 3+2.
	 */
	public static List<Integer> tagli(int carteATesta) {
		List<Integer> tagli = new ArrayList<>();
		int rimaste = carteATesta;
		while (rimaste > 0) {
			int dimensione = Math.min(MAX_CARTE_PER_MANO, rimaste);
			tagli.add(dimensione);
			rimaste -= dimensione;
		}
		return tagli;
	}

	private static List<Carta> scarta(List<Carta> mazzo, int quante) {
		List<Carta> scartate = new ArrayList<>(quante);
		if (quante == 0) {
			return scartate;
		}
		Iterator<Carta> carte = mazzo.iterator();
		while (carte.hasNext() && scartate.size() < quante) {
			Carta carta = carte.next();
			if (carta.valore().figura()) {
				carte.remove();
				scartate.add(carta);
			}
		}
		while (scartate.size() < quante) {
			scartate.add(mazzo.removeLast());
		}
		return scartate;
	}

}
