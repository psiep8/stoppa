package it.stoppa.backend.lobby;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import it.stoppa.backend.tavolo.PostoTavolo;
import it.stoppa.backend.tavolo.StatoTavolo;
import it.stoppa.backend.tavolo.TavoloService;

/**
 * Le stanze della lobby: restano in memoria, hanno un codice di quattro lettere
 * e partono con te e cinque bot.
 *
 * Chi crea la stanza e' l'unico che puo' aggiungere bot, togliere posti e
 * avviare la partita; gli altri entrano con il codice e ricevono una sessione
 * propria. Ogni posto tiene l'id della sessione che lo occupa: il posto di un
 * giocatore si trova cercando quell'id, non un numero.
 */
@Service
public class LobbyService {

	public static final int POSTI_TOTALI = 8;

	public static final int MINIMO_GIOCATORI = 4;

	public static final int BOT_INIZIALI = 5;

	private static final String LETTERE = "ABCDEFGHJKLMNPQRSTUVWXYZ";

	private final Map<String, Stanza> stanze = new ConcurrentHashMap<>();

	private final SessioniService sessioni;

	private final TavoloService tavolo;

	private final ApplicationEventPublisher eventi;

	public LobbyService(SessioniService sessioni, TavoloService tavolo, ApplicationEventPublisher eventi) {
		this.sessioni = sessioni;
		this.tavolo = tavolo;
		this.eventi = eventi;
	}

	public synchronized IngressoLobby crea(String nome) {
		String pulito = pulisciNome(nome);
		Stanza stanza = new Stanza(codiceLibero());
		Sessione sessione = sessioni.crea(pulito, stanza.codice, true);
		stanza.posti.add(new Posto(sessione.id(), pulito));
		for (int i = 1; i <= BOT_INIZIALI; i++) {
			stanza.posti.add(new Posto(null, "Bot " + i));
		}
		stanze.put(stanza.codice, stanza);
		annuncia(stanza);
		return ingresso(sessione, stanza);
	}

	public synchronized IngressoLobby entra(String codice, String nome) {
		Stanza stanza = cerca(codice);
		verificaAvviata(stanza);
		if (stanza.posti.size() >= POSTI_TOTALI) {
			throw new IllegalStateException("il tavolo e' pieno: " + POSTI_TOTALI + " posti");
		}
		String pulito = pulisciNome(nome);
		Sessione sessione = sessioni.crea(pulito, stanza.codice, false);
		stanza.posti.add(new Posto(sessione.id(), pulito));
		annuncia(stanza);
		return ingresso(sessione, stanza);
	}

	public synchronized StanzaLobby stanza(String codice, String sessioneId) {
		Stanza stanza = cerca(codice);
		Sessione sessione = sessioneId == null || sessioneId.isBlank() ? null
				: appartenente(stanza, sessioni.richiesta(sessioneId));
		return vista(stanza, sessione);
	}

	public synchronized StanzaLobby aggiungiBot(String codice, String sessioneId) {
		Stanza stanza = cerca(codice);
		Sessione sessione = creatore(stanza, sessioneId);
		verificaAvviata(stanza);
		if (stanza.posti.size() >= POSTI_TOTALI) {
			throw new IllegalStateException("il tavolo e' pieno: " + POSTI_TOTALI + " posti");
		}
		int bot = (int) stanza.posti.stream().filter(Posto::bot).count();
		stanza.posti.add(new Posto(null, "Bot " + (bot + 1)));
		annuncia(stanza);
		return vista(stanza, sessione);
	}

	public synchronized StanzaLobby togliPosto(String codice, int indice, String sessioneId) {
		Stanza stanza = cerca(codice);
		Sessione sessione = creatore(stanza, sessioneId);
		verificaAvviata(stanza);
		Posto posto = posto(stanza, indice);
		if (sessione.id().equals(posto.sessioneId)) {
			throw new IllegalStateException("il tuo posto non si toglie");
		}
		stanza.posti.remove(indice);
		if (posto.sessioneId != null) {
			sessioni.rimuovi(posto.sessioneId);
		}
		annuncia(stanza);
		return vista(stanza, sessione);
	}

	public synchronized StatoTavolo avvia(String codice, String sessioneId) {
		Stanza stanza = cerca(codice);
		Sessione sessione = creatore(stanza, sessioneId);
		verificaAvviata(stanza);
		if (stanza.posti.size() < MINIMO_GIOCATORI) {
			throw new IllegalStateException("servono almeno " + MINIMO_GIOCATORI + " giocatori");
		}
		List<PostoTavolo> posti = stanza.posti.stream().map(posto -> new PostoTavolo(posto.sessioneId, posto.nome))
				.toList();
		tavolo.crea(stanza.codice, posti);
		stanza.avviata = true;
		annuncia(stanza);
		return tavolo.stato(stanza.codice, sessione.id());
	}

	/** Il socket della stanza manda la vista aggiornata a chi e' collegato. */
	private void annuncia(Stanza stanza) {
		eventi.publishEvent(new StanzaCambiata(stanza.codice));
	}

	private Sessione appartenente(Stanza stanza, Sessione sessione) {
		if (!sessione.codiceStanza().equals(stanza.codice)) {
			throw new IllegalStateException("non sei seduto in questa stanza");
		}
		return sessione;
	}

	private Sessione creatore(Stanza stanza, String sessioneId) {
		Sessione sessione = sessioni.richiesta(sessioneId);
		if (!sessione.codiceStanza().equals(stanza.codice)) {
			throw new IllegalStateException("non sei seduto in questa stanza");
		}
		if (!sessione.creatore()) {
			throw new IllegalStateException("solo chi crea la lobby puo' farlo");
		}
		return sessione;
	}

	private void verificaAvviata(Stanza stanza) {
		if (stanza.avviata) {
			throw new IllegalStateException("la partita e' gia' iniziata");
		}
	}

	private IngressoLobby ingresso(Sessione sessione, Stanza stanza) {
		return new IngressoLobby(sessione.id(), vista(stanza, sessione));
	}

	private StanzaLobby vista(Stanza stanza, Sessione sessione) {
		List<PostoLobby> posti = new ArrayList<>(stanza.posti.size());
		int tuo = -1;
		for (int i = 0; i < stanza.posti.size(); i++) {
			Posto posto = stanza.posti.get(i);
			posti.add(new PostoLobby(i, posto.nome, posto.bot()));
			if (sessione != null && sessione.id().equals(posto.sessioneId)) {
				tuo = i;
			}
		}
		return new StanzaLobby(stanza.codice, List.copyOf(posti), POSTI_TOTALI, MINIMO_GIOCATORI, stanza.avviata, tuo);
	}

	private Posto posto(Stanza stanza, int indice) {
		if (indice < 0 || indice >= stanza.posti.size()) {
			throw new IllegalStateException("posto " + indice + " inesistente");
		}
		return stanza.posti.get(indice);
	}

	private Stanza cerca(String codice) {
		if (codice == null || codice.isBlank()) {
			throw new IllegalStateException("codice stanza mancante");
		}
		Stanza stanza = stanze.get(codice.toUpperCase(Locale.ROOT));
		if (stanza == null) {
			throw new RicominciaException("stanza " + codice + " non trovata");
		}
		return stanza;
	}

	private String codiceLibero() {
		for (int tentativo = 0; tentativo < 200; tentativo++) {
			StringBuilder codice = new StringBuilder(4);
			for (int i = 0; i < 4; i++) {
				codice.append(LETTERE.charAt(ThreadLocalRandom.current().nextInt(LETTERE.length())));
			}
			if (!stanze.containsKey(codice.toString())) {
				return codice.toString();
			}
		}
		throw new IllegalStateException("nessun codice stanza libero");
	}

	private static String pulisciNome(String nome) {
		String pulito = nome == null || nome.isBlank() ? "Tu" : nome.trim();
		return pulito.length() > 14 ? pulito.substring(0, 14) : pulito;
	}

	private static final class Stanza {

		private final String codice;

		private final List<Posto> posti = new ArrayList<>();

		private boolean avviata;

		private Stanza(String codice) {
			this.codice = codice;
		}

	}

	private static final class Posto {

		private final String sessioneId;

		private final String nome;

		private Posto(String sessioneId, String nome) {
			this.sessioneId = sessioneId;
			this.nome = nome;
		}

		private boolean bot() {
			return sessioneId == null;
		}

	}

}
