package it.stoppa.backend.game;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Una mano di gioco: le carte di ognuno (al massimo tre) e il giro di puntate.
 *
 * Regole del giro: parte il cartaro e si gira in senso orario. Si puo' passare
 * (si esce dalla mano perdendo quello che si e' gia' messo), vedere (pareggiare
 * la puntata piu' alta) o puntare di piu'. Le puntate si chiudono quando tutti
 * quelli rimasti in gioco hanno messo lo stesso numero di gettoni, oppure
 * quando ne resta uno solo. Il massimo per puntata e' di dieci gettoni.
 */
public final class Mano {

	public static final int TETTO_PUNTATA = 10;

	public enum Fase {
		PUNTATE, CHIUSA
	}

	private final int numero;

	private final int cartaro;

	private final List<Giocatore> giocatori;

	private final List<List<Carta>> carte;

	private final List<Integer> ordine;

	private final Set<Integer> attivi = new LinkedHashSet<>();

	private final Map<Integer, Integer> puntate = new LinkedHashMap<>();

	private final List<Integer> ordinePuntate = new ArrayList<>();

	private int indiceTurno;

	private int puntataMassima;

	private Fase fase = Fase.PUNTATE;

	private boolean senzaPuntate;

	private List<Integer> vincitori = List.of();

	private List<Carta> carteVincenti = List.of();

	public Mano(int numero, int cartaro, List<Giocatore> giocatori, List<List<Carta>> carte) {
		if (giocatori.size() < 2) {
			throw new IllegalArgumentException("servono almeno due giocatori");
		}
		if (giocatori.size() != carte.size()) {
			throw new IllegalArgumentException("carte non allineate ai giocatori");
		}
		this.numero = numero;
		this.giocatori = List.copyOf(giocatori);
		this.cartaro = Math.floorMod(cartaro, giocatori.size());
		this.carte = carte.stream().map(List::copyOf).toList();
		this.ordine = new ArrayList<>(giocatori.size());
		for (int i = 0; i < giocatori.size(); i++) {
			this.ordine.add((this.cartaro + i) % giocatori.size());
		}
		this.attivi.addAll(ordine);
	}

	public int numero() {
		return numero;
	}

	public int cartaro() {
		return cartaro;
	}

	public Fase fase() {
		return fase;
	}

	public boolean chiusa() {
		return fase == Fase.CHIUSA;
	}

	public boolean senzaPuntate() {
		return senzaPuntate;
	}

	public List<Integer> vincitori() {
		return vincitori;
	}

	public List<Carta> carteVincenti() {
		return carteVincenti;
	}

	public List<Carta> carteDi(int giocatore) {
		return carte.get(giocatore);
	}

	public Map<Integer, Integer> puntate() {
		return Map.copyOf(puntate);
	}

	public int totalePuntate() {
		return puntate.values().stream().mapToInt(Integer::intValue).sum();
	}

	public int puntataMassima() {
		return puntataMassima;
	}

	public Set<Integer> attivi() {
		return Set.copyOf(attivi);
	}

	public int giocatoreDiTurno() {
		return ordine.get(indiceTurno);
	}

	public void passa() {
		verificaAperta();
		int giocatore = giocatoreDiTurno();
		attivi.remove(giocatore);
		if (attivi.isEmpty()) {
			chiudiSenzaPuntate();
			return;
		}
		avanza();
	}

	public void vedo() {
		verificaAperta();
		if (puntataMassima == 0) {
			throw new IllegalStateException("non c'e' ancora niente da vedere: punta almeno un gettone o passa");
		}
		int giocatore = giocatoreDiTurno();
		int dovuto = puntataMassima - puntate.getOrDefault(giocatore, 0);
		paga(giocatore, dovuto);
		avanza();
	}

	public void punta(int gettoni) {
		verificaAperta();
		if (gettoni < 1 || gettoni > TETTO_PUNTATA) {
			throw new IllegalArgumentException("si punta da 1 a " + TETTO_PUNTATA + " gettoni, non " + gettoni);
		}
		int giocatore = giocatoreDiTurno();
		int gia = puntate.getOrDefault(giocatore, 0);
		if (gettoni <= gia) {
			throw new IllegalArgumentException("hai gia' messo " + gia + " gettoni: per restare si usa vedo");
		}
		if (gettoni <= puntataMassima) {
			throw new IllegalArgumentException("per pareggiare la puntata di " + puntataMassima + " si usa vedo");
		}
		paga(giocatore, gettoni - gia);
		puntataMassima = gettoni;
		ordinePuntate.add(giocatore);
		avanza();
	}

	private void verificaAperta() {
		if (fase != Fase.PUNTATE) {
			throw new IllegalStateException("la mano " + numero + " e' gia' chiusa");
		}
	}

	private void paga(int giocatore, int gettoni) {
		if (gettoni <= 0) {
			return;
		}
		giocatori.get(giocatore).scalaFiches(gettoni);
		puntate.merge(giocatore, gettoni, Integer::sum);
	}

	private void avanza() {
		if (puntataMassima == 0) {
			if (attivi.isEmpty()) {
				chiudiSenzaPuntate();
				return;
			}
		}
		else if (attivi.size() == 1) {
			vince(attivi.iterator().next());
			return;
		}
		else if (tuttiPareggiati()) {
			rivela();
			return;
		}

		int prossimo = prossimoTurno();
		if (prossimo < 0) {
			if (puntataMassima > 0) {
				rivela();
			}
			else {
				chiudiSenzaPuntate();
			}
			return;
		}
		indiceTurno = prossimo;
	}

	private int prossimoTurno() {
		for (int salto = 1; salto <= ordine.size(); salto++) {
			int indice = (indiceTurno + salto) % ordine.size();
			if (attivi.contains(ordine.get(indice))) {
				return indice;
			}
		}
		return -1;
	}

	private boolean tuttiPareggiati() {
		if (puntataMassima == 0) {
			return false;
		}
		for (int giocatore : attivi) {
			if (puntate.getOrDefault(giocatore, 0) != puntataMassima) {
				return false;
			}
		}
		return true;
	}

	private void rivela() {
		List<Integer> candidati = new ArrayList<>(attivi);
		int migliore = -1;
		for (int giocatore : candidati) {
			migliore = Math.max(migliore, Punteggio.calcola(carte.get(giocatore)));
		}
		int vincitore = candidati.getFirst();
		List<Integer> pari = new ArrayList<>();
		for (int giocatore : candidati) {
			if (Punteggio.calcola(carte.get(giocatore)) == migliore) {
				pari.add(giocatore);
			}
		}
		if (pari.size() > 1) {
			for (int giocatore : ordinePuntate) {
				if (pari.contains(giocatore)) {
					vincitore = giocatore;
					break;
				}
			}
		}
		vince(vincitore);
	}

	private void vince(int giocatore) {
		vincitori = List.of(giocatore);
		carteVincenti = carte.get(giocatore);
		fase = Fase.CHIUSA;
	}

	private void chiudiSenzaPuntate() {
		senzaPuntate = true;
		vincitori = List.of();
		carteVincenti = List.of();
		fase = Fase.CHIUSA;
	}

}
