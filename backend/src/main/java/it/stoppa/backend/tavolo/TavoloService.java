package it.stoppa.backend.tavolo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import it.stoppa.backend.game.Giocatore;
import it.stoppa.backend.game.Mano;
import it.stoppa.backend.game.Partita;
import it.stoppa.backend.game.RiepilogoPartita;
import it.stoppa.backend.lobby.Sessione;
import it.stoppa.backend.lobby.SessioniService;

/**
 * I tavoli delle stanze: una partita per codice stanza, in memoria.
 *
 * Il posto non arriva mai dal client: si cerca l'id della sessione tra i posti
 * del tavolo, quindi le carte si mostrano solo a chi quel posto lo occupa
 * davvero. Gli altri vedono solo quante carte ha ognuno.
 *
 * Dopo ogni mossa i bot giocano fino al turno di un giocatore umano, cosi' chi
 * chiama riceve sempre uno stato pronto da mostrare.
 */
@Service
public class TavoloService {

	private final Map<String, PartitaTavolo> tavoli = new ConcurrentHashMap<>();

	private final SessioniService sessioni;

	private final ApplicationEventPublisher eventi;

	public TavoloService(SessioniService sessioni, ApplicationEventPublisher eventi) {
		this.sessioni = sessioni;
		this.eventi = eventi;
	}

	public synchronized void crea(String codice, List<PostoTavolo> posti) {
		String stanza = chiave(codice);
		if (tavoli.containsKey(stanza)) {
			throw new IllegalStateException("la stanza " + stanza + " ha gia' una partita");
		}
		PartitaTavolo tavolo = new PartitaTavolo(stanza, posti);
		tavoli.put(stanza, tavolo);
		aggiungiEvento(tavolo, "Carte distribuite · " + tavolo.partita.maniTotali() + " mani");
		giocaBot(tavolo);
		if (tavolo.partita.mano().chiusa()) {
			aggiungiEvento(tavolo, esitoMano(tavolo));
		}
	}

	public synchronized StatoTavolo stato(String codice, String sessioneId) {
		Accesso accesso = accedi(codice, sessioneId);
		return statoPer(accesso);
	}

	/**
	 * Fotografia per chi crea la lobby: gettoni, debiti, piatto e punto di ogni
	 * giocatore.
	 */
	public synchronized RiepilogoPartita riepilogo(String codice, String sessioneId) {
		Accesso accesso = accedi(codice, sessioneId);
		if (!accesso.sessione().creatore()) {
			throw new IllegalStateException("il riepilogo e' solo di chi crea la lobby");
		}
		return accesso.tavolo.partita.riepilogo();
	}

	public synchronized StatoTavolo passa(String codice, String sessioneId) {
		Accesso accesso = accessoDiTurno(codice, sessioneId);
		accesso.tavolo.partita.passa();
		aggiungiEvento(accesso.tavolo, "Passi");
		return dopoLaMossa(accesso);
	}

	public synchronized StatoTavolo vedo(String codice, String sessioneId) {
		Accesso accesso = accessoDiTurno(codice, sessioneId);
		accesso.tavolo.partita.vedo();
		aggiungiEvento(accesso.tavolo, "Vedi");
		return dopoLaMossa(accesso);
	}

	public synchronized StatoTavolo punta(String codice, String sessioneId, int gettoni) {
		Accesso accesso = accessoDiTurno(codice, sessioneId);
		accesso.tavolo.partita.punta(gettoni);
		aggiungiEvento(accesso.tavolo, "Punti " + gettoni);
		return dopoLaMossa(accesso);
	}

	public synchronized StatoTavolo prossimaMano(String codice, String sessioneId) {
		Accesso accesso = accedi(codice, sessioneId);
		Partita partita = accesso.tavolo.partita;
		if (partita.finita()) {
			return statoPer(accesso);
		}
		if (!partita.mano().chiusa()) {
			throw new IllegalStateException("la mano " + partita.numeroMano() + " e' ancora aperta");
		}
		partita.prossimaMano();
		if (partita.finita()) {
			Giocatore primo = partita.giocatori().get(partita.classifica().getFirst());
			aggiungiEvento(accesso.tavolo,
					"Partita finita · vince " + primo.nome() + " con " + primo.punteggio() + " punti");
			return annuncia(accesso);
		}
		aggiungiEvento(accesso.tavolo,
				"Mano " + partita.numeroMano() + " · cartaro " + partita.giocatori().get(partita.cartaro()).nome());
		giocaBot(accesso.tavolo);
		if (partita.mano().chiusa()) {
			aggiungiEvento(accesso.tavolo, esitoMano(accesso.tavolo));
		}
		return annuncia(accesso);
	}

	public synchronized StatoTavolo nuovaPartita(String codice, String sessioneId) {
		Accesso accesso = accedi(codice, sessioneId);
		if (!accesso.sessione().creatore()) {
			throw new IllegalStateException("solo chi crea la lobby puo' ricominciare");
		}
		PartitaTavolo tavolo = accesso.tavolo;
		tavolo.partita = tavolo.nuovaPartita();
		tavolo.eventi.clear();
		aggiungiEvento(tavolo, "Carte distribuite · " + tavolo.partita.maniTotali() + " mani");
		giocaBot(tavolo);
		if (tavolo.partita.mano().chiusa()) {
			aggiungiEvento(tavolo, esitoMano(tavolo));
		}
		return annuncia(accesso);
	}

	/**
	 * Lo stato per chi ha mosso, annunciato alla stanza: REST e socket passano
	 * entrambi di qui, quindi ogni mossa riuscita arriva a tutti i presenti.
	 */
	private StatoTavolo annuncia(Accesso accesso) {
		eventi.publishEvent(new StatoTavoloCambiato(accesso.tavolo.codice));
		return statoPer(accesso);
	}

	private StatoTavolo dopoLaMossa(Accesso accesso) {
		if (!accesso.tavolo.partita.mano().chiusa()) {
			giocaBot(accesso.tavolo);
		}
		if (accesso.tavolo.partita.mano().chiusa()) {
			aggiungiEvento(accesso.tavolo, esitoMano(accesso.tavolo));
		}
		return annuncia(accesso);
	}

	private Accesso accedi(String codice, String sessioneId) {
		Sessione sessione = sessioni.richiesta(sessioneId);
		String stanza = chiave(codice);
		if (!sessione.codiceStanza().equals(stanza)) {
			throw new IllegalStateException("non sei seduto nella stanza " + stanza);
		}
		PartitaTavolo tavolo = tavoli.get(stanza);
		if (tavolo == null) {
			throw new IllegalStateException("la stanza " + stanza + " non ha una partita avviata");
		}
		return new Accesso(tavolo, sessione, tavolo.indiceDi(sessione.id()));
	}

	private static String chiave(String codice) {
		return codice == null ? "" : codice.toUpperCase(Locale.ROOT);
	}

	private Accesso accessoDiTurno(String codice, String sessioneId) {
		Accesso accesso = accedi(codice, sessioneId);
		if (accesso.tavolo.partita.finita()) {
			throw new IllegalStateException("la partita e' finita: comincia una nuova partita");
		}
		if (accesso.tavolo.partita.mano().chiusa()) {
			throw new IllegalStateException("la mano e' chiusa: passa alla prossima");
		}
		if (accesso.tavolo.partita.mano().giocatoreDiTurno() != accesso.indice()) {
			throw new IllegalStateException("non e' il tuo turno");
		}
		return accesso;
	}

	private void giocaBot(PartitaTavolo tavolo) {
		int guardia = 0;
		while (!tavolo.partita.finita() && !tavolo.partita.mano().chiusa()
				&& tavolo.posti.get(tavolo.partita.mano().giocatoreDiTurno()).bot() && guardia++ < 200) {
			mossaBot(tavolo, tavolo.partita.mano().giocatoreDiTurno());
		}
	}

	private void mossaBot(PartitaTavolo tavolo, int indice) {
		Mano mano = tavolo.partita.mano();
		String nome = tavolo.partita.giocatori().get(indice).nome();
		int massima = mano.puntataMassima();
		int gia = mano.puntate().getOrDefault(indice, 0);

		if (massima == 0) {
			if (tavolo.random.nextInt(100) < 20) {
				tavolo.partita.passa();
				aggiungiEvento(tavolo, nome + " passa");
				return;
			}
			int gettoni = 1 + tavolo.random.nextInt(3);
			tavolo.partita.punta(gettoni);
			aggiungiEvento(tavolo, nome + " punta " + gettoni);
			return;
		}

		int scelta = tavolo.random.nextInt(100);
		if (scelta < 20) {
			tavolo.partita.passa();
			aggiungiEvento(tavolo, nome + " passa");
			return;
		}
		if (scelta < 85 || massima >= Mano.TETTO_PUNTATA || gia >= massima) {
			tavolo.partita.vedo();
			aggiungiEvento(tavolo, nome + " vede");
			return;
		}
		int gettoni = Math.min(massima + 1 + tavolo.random.nextInt(2), Mano.TETTO_PUNTATA);
		tavolo.partita.punta(gettoni);
		aggiungiEvento(tavolo, nome + " rilancia a " + gettoni);
	}

	private String esitoMano(PartitaTavolo tavolo) {
		Mano mano = tavolo.partita.mano();
		if (mano.senzaPuntate()) {
			return "Mano " + mano.numero() + ": nessuno ha puntato, 1 gettone al cartaro";
		}
		String vincitore = tavolo.partita.giocatori().get(mano.vincitori().getFirst()).nome();
		return "Mano " + mano.numero() + ": vince " + vincitore;
	}

	private StatoTavolo statoPer(Accesso accesso) {
		PartitaTavolo tavolo = accesso.tavolo;
		int indice = accesso.indice();
		Partita partita = tavolo.partita;
		Mano mano = partita.mano();
		List<GiocatoreTavolo> giocatori = new ArrayList<>();
		for (int i = 0; i < partita.giocatori().size(); i++) {
			Giocatore giocatore = partita.giocatori().get(i);
			boolean sonoIo = i == indice;
			giocatori.add(new GiocatoreTavolo(i, giocatore.nome(), giocatore.fiches(), giocatore.debito(),
					mano.puntate().getOrDefault(i, 0), mano.attivi().contains(i),
					!mano.chiusa() && mano.giocatoreDiTurno() == i, tavolo.posti.get(i).bot(), mano.carteDi(i).size(),
					sonoIo ? carte(mano.carteDi(i)) : List.of()));
		}

		ManoTavolo manoTavolo = new ManoTavolo(mano.numero(), partita.maniTotali(),
				partita.giocatori().get(mano.cartaro()).nome(), partita.piatto(), mano.puntataMassima(), mano.chiusa(),
				mano.senzaPuntate(),
				mano.vincitori().stream().map(i -> partita.giocatori().get(i).nome()).toList(),
				carte(mano.carteVincenti()));

		boolean toccaATe = !partita.finita() && !mano.chiusa() && mano.giocatoreDiTurno() == indice;

		return new StatoTavolo(indice, tavolo.posti.get(indice).nome(), giocatori, manoTavolo, toccaATe,
				partita.finita(), List.copyOf(tavolo.eventi), partita.finita() ? classifica(tavolo) : List.of(),
				mano.puntataMassima() + 1, Mano.TETTO_PUNTATA);
	}

	private List<ClassificaTavolo> classifica(PartitaTavolo tavolo) {
		List<ClassificaTavolo> righe = new ArrayList<>();
		int posizione = 1;
		for (int indice : tavolo.partita.classifica()) {
			Giocatore giocatore = tavolo.partita.giocatori().get(indice);
			righe.add(new ClassificaTavolo(posizione++, giocatore.nome(), giocatore.punteggio(), giocatore.fiches()));
		}
		return righe;
	}

	private static List<CartaTavolo> carte(List<it.stoppa.backend.game.Carta> carte) {
		return carte.stream().map(CartaTavolo::da).toList();
	}

	private void aggiungiEvento(PartitaTavolo tavolo, String testo) {
		tavolo.eventi.addFirst(testo);
		while (tavolo.eventi.size() > PartitaTavolo.MAX_EVENTI) {
			tavolo.eventi.removeLast();
		}
	}

	private record Accesso(PartitaTavolo tavolo, Sessione sessione, int indice) {
	}

}
