package it.stoppa.backend.lobby;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;

import it.stoppa.backend.tavolo.StatoTavolo;
import it.stoppa.backend.tavolo.TavoloService;

/**
 * Le stanze della lobby: restano in memoria, hanno un codice di quattro lettere
 * e partono con te e cinque bot gia' seduti.
 */
@Service
public class LobbyService {

	public static final int POSTI_TOTALI = 8;

	public static final int MINIMO_GIOCATORI = 4;

	public static final int BOT_INIZIALI = 5;

	private static final String LETTERE = "ABCDEFGHJKLMNPQRSTUVWXYZ";

	private final Map<String, Stanza> stanze = new ConcurrentHashMap<>();

	private final TavoloService tavolo;

	public LobbyService(TavoloService tavolo) {
		this.tavolo = tavolo;
	}

	public synchronized StanzaLobby crea(String nome) {
		String pulito = nome == null || nome.isBlank() ? "Tu" : nome.trim();
		if (pulito.length() > 14) {
			pulito = pulito.substring(0, 14);
		}
		List<PostoLobby> posti = new ArrayList<>();
		posti.add(new PostoLobby(0, pulito, false));
		for (int i = 1; i <= BOT_INIZIALI; i++) {
			posti.add(new PostoLobby(i, "Bot " + i, true));
		}
		Stanza stanza = new Stanza(codiceLibero(), posti);
		stanze.put(stanza.codice, stanza);
		return stanza.inLobby();
	}

	public synchronized StanzaLobby stanza(String codice) {
		return cerca(codice).inLobby();
	}

	public synchronized StanzaLobby aggiungiBot(String codice) {
		Stanza stanza = cerca(codice);
		if (stanza.posti.size() >= POSTI_TOTALI) {
			throw new IllegalStateException("il tavolo e' pieno: " + POSTI_TOTALI + " posti");
		}
		int bot = (int) stanza.posti.stream().filter(PostoLobby::bot).count();
		stanza.posti.add(new PostoLobby(stanza.posti.size(), "Bot " + (bot + 1), true));
		return stanza.inLobby();
	}

	public synchronized StanzaLobby togliPosto(String codice, int indice) {
		Stanza stanza = cerca(codice);
		if (indice <= 0) {
			throw new IllegalStateException("il tuo posto non si toglie");
		}
		stanza.posti.removeIf(posto -> posto.indice() == indice);
		List<PostoLobby> riordinati = new ArrayList<>();
		for (int i = 0; i < stanza.posti.size(); i++) {
			PostoLobby posto = stanza.posti.get(i);
			riordinati.add(new PostoLobby(i, posto.nome(), posto.bot()));
		}
		stanza.posti = riordinati;
		return stanza.inLobby();
	}

	public synchronized StatoTavolo avvia(String codice) {
		Stanza stanza = cerca(codice);
		if (stanza.posti.size() < MINIMO_GIOCATORI) {
			throw new IllegalStateException("servono almeno " + MINIMO_GIOCATORI + " giocatori");
		}
		stanza.avviata = true;
		return tavolo.nuovaPartita(stanza.posti.stream().map(PostoLobby::nome).toList());
	}

	private Stanza cerca(String codice) {
		if (codice == null) {
			throw new IllegalStateException("codice stanza mancante");
		}
		Stanza stanza = stanze.get(codice.toUpperCase(Locale.ROOT));
		if (stanza == null) {
			throw new IllegalStateException("stanza " + codice + " non trovata");
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

	private static final class Stanza {

		private final String codice;

		private List<PostoLobby> posti;

		private boolean avviata;

		private Stanza(String codice, List<PostoLobby> posti) {
			this.codice = codice;
			this.posti = posti;
		}

		private StanzaLobby inLobby() {
			return new StanzaLobby(codice, List.copyOf(posti), POSTI_TOTALI, MINIMO_GIOCATORI);
		}

	}

}
