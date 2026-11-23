package it.stoppa.backend.lobby;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import it.stoppa.backend.tavolo.StatoTavolo;
import it.stoppa.backend.tavolo.StatoTavoloCambiato;
import it.stoppa.backend.tavolo.TavoloService;
import tools.jackson.databind.json.JsonMapper;

/**
 * Il socket della stanza, non del tavolo.
 *
 * Si entra con {@code {"tipo":"entra","codice":"ABCD","sessione":"..."}} e da
 * li' in poi il server manda due tipi di messaggi: {@code stanza} quando cambia
 * la lobby (ingressi, bot, posti, avvio) e {@code stato} quando qualcuno muove,
 * filtrato per la sessione di ognuno. La lobby e la partita usano lo stesso
 * canale, quindi non serve interrogare il server per sapere cosa succede.
 *
 * Dopo {@code entra} ogni messaggio e' una mossa ({@code passa}, {@code vedo},
 * {@code punta} con {@code gettoni}, {@code prossima-mano}, {@code nuova-partita}).
 * {@code {"tipo":"stato"}} chiede una risincronizzazione.
 */
@Component
public class LobbyWebSocketHandler extends TextWebSocketHandler {

	private static final int TIMEOUT_INVIIO_MS = 5000;

	private static final int BUFFER_INVIIO = 64 * 1024;

	/** Ogni socket collegato, anche prima che entri in una stanza. */
	private final Map<String, Collegamento> sockets = new ConcurrentHashMap<>();

	/** I socket di ogni stanza, per sessione. */
	private final Map<String, Map<String, Collegamento>> stanze = new ConcurrentHashMap<>();

	/** Chi ha acceso il microfono, per stanza e per sessione. */
	private final Map<String, Map<String, Voce>> voci = new ConcurrentHashMap<>();

	private final LobbyService lobby;

	private final TavoloService tavolo;

	private final JsonMapper json = new JsonMapper();

	public LobbyWebSocketHandler(LobbyService lobby, TavoloService tavolo) {
		this.lobby = lobby;
		this.tavolo = tavolo;
	}

	/** La lobby e' cambiata: la stanza aggiornata va a chi e' seduto qui. */
	@EventListener
	public void stanzaCambiata(StanzaCambiata evento) {
		aOgniMembro(evento.codice(), membro -> sincronizza(membro, lobby.stanza(membro.codice(), membro.sessione())));
	}

	/** Ogni mossa riuscita, da WebSocket o da REST, arriva qui e va a tutta la stanza. */
	@EventListener
	public void statoCambiato(StatoTavoloCambiato evento) {
		aOgniMembro(evento.codice(), membro -> inviaStato(membro, tavolo.stato(evento.codice(), membro.sessione())));
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		WebSocketSession socket = new ConcurrentWebSocketSessionDecorator(session, TIMEOUT_INVIIO_MS, BUFFER_INVIIO);
		sockets.put(session.getId(), new Collegamento(session.getId(), socket));
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		scollega(session.getId());
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) {
		scollega(session.getId());
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) {
		String socketId = session.getId();
		try {
			Messaggio messaggio = json.readValue(message.getPayload(), Messaggio.class);
			String tipo = messaggio.tipo() == null ? "" : messaggio.tipo();
			switch (tipo) {
				case "entra" -> entra(socketId, messaggio);
				case "stato" -> risincronizza(socketId);
				case "passa" -> esegui(socketId, tavolo::passa);
				case "vedo" -> esegui(socketId, tavolo::vedo);
				case "punta" -> eseguiPunta(socketId, messaggio.gettoni());
				case "mostra-fiches" -> eseguiMostraFiches(socketId, messaggio.mostra());
				case "dichiara" -> eseguiDichiara(socketId, messaggio.gettoni());
				case "contesta" -> esegui(socketId, (codice, sessione) -> tavolo.contesta(codice, sessione));
				case "accetta" -> esegui(socketId, (codice, sessione) -> tavolo.accetta(codice, sessione));
				case "prossima-mano" -> esegui(socketId, tavolo::prossimaMano);
				case "nuova-partita" -> esegui(socketId, tavolo::nuovaPartita);
				case "voce-entra" -> voceEntra(socketId);
				case "voce-esce" -> voceEsce(socketId);
				case "voce-muta" -> voceMuta(socketId, messaggio.muto());
				case "voce" -> voceInoltra(socketId, messaggio);
				default -> errore(socketId, new IllegalArgumentException("messaggio sconosciuto: " + tipo));
			}
		} catch (RuntimeException eccezione) {
			errore(socketId, eccezione);
		}
	}

	private void entra(String socketId, Messaggio messaggio) {
		if (messaggio.codice() == null || messaggio.sessione() == null) {
			throw new IllegalStateException("per entrare servono codice stanza e sessione");
		}
		Collegamento socket = sockets.get(socketId);
		if (socket == null) {
			return;
		}
		String codice = messaggio.codice().toUpperCase(Locale.ROOT);
		StanzaLobby stanza = lobby.stanza(codice, messaggio.sessione());
		scollega(socketId);
		Collegamento collegamento = new Collegamento(socketId, socket.socket(), codice, messaggio.sessione());
		Collegamento precedente = stanze.computeIfAbsent(codice, chiave -> new ConcurrentHashMap<>())
				.put(messaggio.sessione(), collegamento);
		if (precedente != null) {
			sockets.remove(precedente.socketId());
		}
		sockets.put(socketId, collegamento);
		Map<String, Voce> vociStanza = voci.get(codice);
		if (vociStanza != null) {
			Voce voce = vociStanza.get(messaggio.sessione());
			if (voce != null) {
				vociStanza.put(messaggio.sessione(), new Voce(voce.id(), collegamento));
				annunciaVoci(codice);
			}
		}
		sincronizza(collegamento, stanza);
	}

	private void risincronizza(String socketId) {
		Collegamento collegamento = collegamento(socketId);
		sincronizza(collegamento, lobby.stanza(collegamento.codice(), collegamento.sessione()));
	}

	/** Manda la stanza a un membro; se la partita e' partita manda anche lo stato. */
	private void sincronizza(Collegamento membro, StanzaLobby stanza) {
		inviaStanza(membro, stanza);
		if (stanza.avviata()) {
			inviaStato(membro, tavolo.stato(membro.codice(), membro.sessione()));
		}
	}

	private void esegui(String socketId, Mossa mossa) {
		Collegamento collegamento = collegamento(socketId);
		mossa.esegui(collegamento.codice(), collegamento.sessione());
	}

	private void eseguiDichiara(String socketId, Integer punti) {
		if (punti == null) {
			throw new IllegalArgumentException("manca il punteggio dichiarato");
		}
		esegui(socketId, (codice, sessione) -> tavolo.dichiara(codice, sessione, punti));
	}

	private void eseguiMostraFiches(String socketId, Boolean mostra) {
		esegui(socketId, (codice, sessione) -> tavolo.mostraFiches(codice, sessione, Boolean.TRUE.equals(mostra)));
	}

	private void eseguiPunta(String socketId, Integer gettoni) {
		if (gettoni == null) {
			throw new IllegalArgumentException("manca il numero di gettoni");
		}
		esegui(socketId, (codice, sessione) -> tavolo.punta(codice, sessione, gettoni));
	}

	/** Un evento della stanza va a ogni membro; un membro rotto riceve l'errore. */
	private void aOgniMembro(String codice, Consumer<Collegamento> azione) {
		Map<String, Collegamento> membri = stanze.get(codice);
		if (membri == null) {
			return;
		}
		for (Collegamento membro : membri.values()) {
			try {
				azione.accept(membro);
			} catch (RuntimeException eccezione) {
				inviaErrore(membro, eccezione);
			}
		}
	}

	private Collegamento collegamento(String socketId) {
		Collegamento collegamento = sockets.get(socketId);
		if (collegamento == null || collegamento.codice() == null) {
			throw new IllegalStateException("prima entra con il codice stanza");
		}
		return collegamento;
	}

	private void scollega(String socketId) {
		Collegamento collegamento = sockets.remove(socketId);
		if (collegamento == null || collegamento.codice() == null) {
			return;
		}
		if (togliVoce(collegamento)) {
			annunciaVoci(collegamento.codice());
		}
		Map<String, Collegamento> membri = stanze.get(collegamento.codice());
		if (membri == null) {
			return;
		}
		membri.remove(collegamento.sessione(), collegamento);
		if (membri.isEmpty()) {
			stanze.remove(collegamento.codice(), membri);
		}
	}

	/* ---------------- voce ---------------- */

	/**
	 * La voce non passa dal server: qui si scambiano solo offerte, risposte e
	 * candidati ICE. Ogni partecipante ha un id opaco, cosi' la sessione non
	 * esce mai dalla sua connessione.
	 */
	private void voceEntra(String socketId) {
		Collegamento membro = collegamento(socketId);
		Map<String, Voce> stanza = voci.computeIfAbsent(membro.codice(), chiave -> new ConcurrentHashMap<>());
		stanza.computeIfAbsent(membro.sessione(), chiave -> new Voce(nuovoIdVoce(), membro));
		annunciaVoci(membro.codice());
	}

	private void voceEsce(String socketId) {
		Collegamento membro = collegamento(socketId);
		if (togliVoce(membro)) {
			annunciaVoci(membro.codice());
		}
	}

	private void voceMuta(String socketId, Boolean muto) {
		Collegamento membro = collegamento(socketId);
		Map<String, Voce> stanza = voci.get(membro.codice());
		Voce voce = stanza == null ? null : stanza.get(membro.sessione());
		if (voce == null) {
			throw new IllegalStateException("prima accendi il microfono");
		}
		voce.muto(muto != null && muto);
		annunciaVoci(membro.codice());
	}

	/** Un messaggio di segnalazione va solo al peer indicato, dentro la stanza. */
	private void voceInoltra(String socketId, Messaggio messaggio) {
		Collegamento membro = collegamento(socketId);
		if (messaggio.a() == null || messaggio.dati() == null) {
			throw new IllegalArgumentException("manca il destinatario o il contenuto");
		}
		Map<String, Voce> stanza = voci.get(membro.codice());
		Voce mia = stanza == null ? null : stanza.get(membro.sessione());
		if (mia == null) {
			throw new IllegalStateException("prima accendi il microfono");
		}
		Voce destinatario = null;
		for (Voce voce : stanza.values()) {
			if (voce.id().equals(messaggio.a())) {
				destinatario = voce;
			}
		}
		if (destinatario == null) {
			return;
		}
		inviaTesto(destinatario.collegamento().socket(),
				json.writeValueAsString(Map.of("tipo", "voce", "da", mia.id(), "dati", messaggio.dati())));
	}

	/** Il roster va a chi ha il microfono acceso: id opaco, nome, se e' muto. */
	private void annunciaVoci(String codice) {
		Map<String, Voce> stanza = voci.get(codice);
		if (stanza == null) {
			return;
		}
		List<Map<String, Object>> partecipanti = new ArrayList<>();
		for (Voce voce : stanza.values()) {
			partecipanti.add(Map.of(
					"id", voce.id(),
					"nome", nome(voce.collegamento()),
					"muto", voce.muto()));
		}
		for (Voce voce : stanza.values()) {
			Map<String, Object> corpo = Map.of(
					"tipo", "voce",
					"mio", voce.id(),
					"partecipanti", partecipanti);
			inviaTesto(voce.collegamento().socket(), json.writeValueAsString(corpo));
		}
	}

	private boolean togliVoce(Collegamento membro) {
		Map<String, Voce> stanza = voci.get(membro.codice());
		if (stanza == null) {
			return false;
		}
		Voce voce = stanza.get(membro.sessione());
		if (voce == null || !voce.collegamento().socketId().equals(membro.socketId())) {
			return false;
		}
		stanza.remove(membro.sessione());
		if (stanza.isEmpty()) {
			voci.remove(membro.codice(), stanza);
		}
		return true;
	}

	/** Il nome si legge dalla lobby, l'unica che sa chi e' seduto dove. */
	private String nome(Collegamento membro) {
		try {
			StanzaLobby stanza = lobby.stanza(membro.codice(), membro.sessione());
			return stanza.posti().stream()
					.filter(posto -> posto.indice() == stanza.tuoIndice())
					.findFirst()
					.map(PostoLobby::nome)
					.orElse("Giocatore");
		} catch (RuntimeException eccezione) {
			return "Giocatore";
		}
	}

	private static String nuovoIdVoce() {
		return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
	}

	private void inviaStato(Collegamento collegamento, StatoTavolo stato) {
		inviaTesto(collegamento.socket(), json.writeValueAsString(Map.of("tipo", "stato", "stato", stato)));
	}

	private void inviaStanza(Collegamento collegamento, StanzaLobby stanza) {
		inviaTesto(collegamento.socket(), json.writeValueAsString(Map.of("tipo", "stanza", "stanza", stanza)));
	}

	/** Solo lo stato salvato non valido porta un motivo: il client torna alla home. */
	private void inviaErrore(Collegamento collegamento, RuntimeException eccezione) {
		String testo = messaggio(eccezione);
		Map<String, Object> corpo = eccezione instanceof RicominciaException
				? Map.of("tipo", "errore", "messaggio", testo, "motivo", "ricomincia")
				: Map.of("tipo", "errore", "messaggio", testo);
		inviaTesto(collegamento.socket(), json.writeValueAsString(corpo));
	}

	private void errore(String socketId, RuntimeException eccezione) {
		Collegamento collegamento = sockets.get(socketId);
		if (collegamento != null) {
			inviaErrore(collegamento, eccezione);
		}
	}

	private void inviaTesto(WebSocketSession socket, String testo) {
		try {
			if (socket.isOpen()) {
				socket.sendMessage(new TextMessage(testo));
			}
		} catch (IOException eccezione) {
			// connessione persa: la pulizia arriva con afterConnectionClosed
		}
	}

	private static String messaggio(RuntimeException eccezione) {
		return eccezione.getMessage() == null ? "richiesta non valida" : eccezione.getMessage();
	}

	public record Messaggio(String tipo, String codice, String sessione, Integer gettoni, String a, Boolean muto,
			Boolean mostra, Map<String, Object> dati) {
	}

	/** Chi ha il microfono acceso: id opaco verso gli altri, socket per parlargli. */
	private static final class Voce {

		private final String id;

		private final Collegamento collegamento;

		private boolean muto;

		private Voce(String id, Collegamento collegamento) {
			this.id = id;
			this.collegamento = collegamento;
		}

		private String id() {
			return id;
		}

		private Collegamento collegamento() {
			return collegamento;
		}

		private boolean muto() {
			return muto;
		}

		private void muto(boolean valore) {
			this.muto = valore;
		}

	}

	/** Un socket collegato: codice e sessione restano null finche' non entra in una stanza. */
	private record Collegamento(String socketId, WebSocketSession socket, String codice, String sessione) {

		private Collegamento(String socketId, WebSocketSession socket) {
			this(socketId, socket, null, null);
		}

	}

	@FunctionalInterface
	private interface Mossa {

		void esegui(String codice, String sessione);

	}

}
