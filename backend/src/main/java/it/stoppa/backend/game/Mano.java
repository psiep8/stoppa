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

	/** Il massimo che si puo' dichiarare a voce: il punto piu' alto possibile. */
	public static final int TETTO_DICHIARAZIONE = Punteggio.MASSIMO;

	/** Quante volte si puo' bluffare e farsi accettare: poi si dice la verita'. */
	public static final int TETTO_BLUFF = 3;

	public enum Fase {
		PUNTATE, DICHIARAZIONE, CHIUSA
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

	/** Chi apre le dichiarazioni: l'ultimo rilancio rimasto in gioco. */
	private int dichiarante = -1;

	/** Quanto ha dichiarato: zero finche' non parla. */
	private int dichiarazione;

	/** Chi ha accettato la dichiarazione senza contestarla. */
	private final Set<Integer> accettato = new LinkedHashSet<>();

	/** Bluff accettati da tutti in questa mano: oltre il tetto si dice il vero. */
	private int bluff;

	/** Come e' finita la dichiarazione: accettata, buono, buono-di-mano, non-sei-buono. */
	private String esito = "";

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

	/** Chi ha puntato, in ordine: serve a sapere chi parla per primo a fine mano. */
	public List<Integer> ordinePuntate() {
		return List.copyOf(ordinePuntate);
	}

	public Set<Integer> attivi() {
		return Set.copyOf(attivi);
	}

	public int giocatoreDiTurno() {
		return ordine.get(indiceTurno);
	}

	public boolean inDichiarazione() {
		return fase == Fase.DICHIARAZIONE;
	}

	public int dichiarante() {
		return dichiarante;
	}

	public int dichiarazione() {
		return dichiarazione;
	}

	public String esitoDichiarazione() {
		return esito;
	}

	public Set<Integer> accettato() {
		return Set.copyOf(accettato);
	}

	public int bluffFatti() {
		return bluff;
	}

	public int bluffRimasti() {
		return TETTO_BLUFF - bluff;
	}

	/**
	 * Le puntate sono chiuse: si passa alle dichiarazioni. Parla l'ultimo che ha
	 * rialzato ed e' ancora in mano, altrimenti chi ha chiamato per primo.
	 */
	private void apriDichiarazione() {
		fase = Fase.DICHIARAZIONE;
		dichiarazione = 0;
		accettato.clear();
		bluff = 0;
		esito = "";
		dichiarante = -1;
		for (int i = ordinePuntate.size() - 1; i >= 0; i--) {
			if (attivi.contains(ordinePuntate.get(i))) {
				dichiarante = ordinePuntate.get(i);
				break;
			}
		}
		if (dichiarante < 0) {
			for (int giocatore : attivi) {
				dichiarante = giocatore;
				break;
			}
		}
	}

	/** Chi apre dice il suo punteggio: puo' essere vero o un bluff. */
	public void dichiara(int giocatore, int punti) {
		if (fase != Fase.DICHIARAZIONE) {
			throw new IllegalStateException("non c'e' una dichiarazione aperta");
		}
		if (giocatore != dichiarante) {
			throw new IllegalStateException("parla solo chi ha aperto la mano");
		}
		if (dichiarazione > 0) {
			throw new IllegalStateException("la dichiarazione e' gia' stata fatta");
		}
		if (punti < 1 || punti > TETTO_DICHIARAZIONE) {
			throw new IllegalArgumentException("si dichiara da 1 a " + TETTO_DICHIARAZIONE + " punti");
		}
		if (bluff >= TETTO_BLUFF && punti != Punteggio.calcola(carte.get(giocatore))) {
			throw new IllegalStateException("hai gia' bluffato " + TETTO_BLUFF + " volte: ora devi dire la verita'");
		}
		this.dichiarazione = punti;
	}

	/**
	 * Un altro giocatore contesta: si girano le carte e vince il punto piu' alto.
	 * Non si contesta per finta: chi ha meno del punto dichiarato non puo' farlo.
	 */
	public void contesta(int giocatore) {
		verificaRisposta(giocatore);
		int suoi = Punteggio.calcola(carte.get(giocatore));
		if (suoi < dichiarazione) {
			throw new IllegalStateException(
					"non puoi contestare: hai " + suoi + " punti contro i " + dichiarazione + " dichiarati");
		}
		int migliore = -1;
		for (int g : attivi) {
			migliore = Math.max(migliore, Punteggio.calcola(carte.get(g)));
		}
		List<Integer> pari = new ArrayList<>();
		for (int g : attivi) {
			if (Punteggio.calcola(carte.get(g)) == migliore) {
				pari.add(g);
			}
		}
		int vincitore = pari.getFirst();
		if (pari.size() > 1) {
			for (int g : ordinePuntate) {
				if (pari.contains(g)) {
					vincitore = g;
					break;
				}
			}
		}
		int puntiVincitore = Punteggio.calcola(carte.get(vincitore));
		boolean dichiaranteAllaPari = Punteggio.calcola(carte.get(dichiarante)) == dichiarazione;
		if (vincitore == dichiarante && puntiVincitore >= dichiarazione) {
			esito = "buono";
		} else if (puntiVincitore == dichiarazione && dichiaranteAllaPari) {
			// stesso punto detto, ma il vincitore viene prima in ordine: "di mano"
			esito = "buono-di-mano";
		} else {
			esito = "non-sei-buono";
		}
		vince(vincitore);
	}

	/**
	 * Un altro giocatore accetta senza contestare. Se accettano tutti: se la
	 * dichiarazione era vera vince chi ha parlato, se era un bluff si torna a
	 * parlare con lui (fino al tetto dei bluff), poi serve la verita'.
	 */
	public void accetta(int giocatore) {
		verificaRisposta(giocatore);
		accettato.add(giocatore);
		for (int g : attivi) {
			if (g != dichiarante && !accettato.contains(g)) {
				return;
			}
		}
		if (dichiarazione == Punteggio.calcola(carte.get(dichiarante))) {
			esito = "buono";
			vince(dichiarante);
			return;
		}
		if (bluff < TETTO_BLUFF) {
			bluff++;
			dichiarazione = 0;
			accettato.clear();
			return;
		}
		esito = "accettata";
		vince(dichiarante);
	}

	private void verificaRisposta(int giocatore) {
		if (fase != Fase.DICHIARAZIONE) {
			throw new IllegalStateException("non c'e' una dichiarazione aperta");
		}
		if (dichiarazione == 0) {
			throw new IllegalStateException("prima parla chi ha aperto la mano");
		}
		if (!attivi.contains(giocatore)) {
			throw new IllegalStateException("non sei piu' in questa mano");
		}
		if (accettato.contains(giocatore)) {
			throw new IllegalStateException("hai gia' accettato: la risposta e' definitiva");
		}
		if (giocatore == dichiarante) {
			throw new IllegalStateException("non puoi rispondere a te stesso");
		}
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
			apriDichiarazione();
			return;
		}

		int prossimo = prossimoTurno();
				if (prossimo < 0) {
			if (puntataMassima > 0) {
				apriDichiarazione();
			} else {
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
