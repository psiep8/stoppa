package it.stoppa.backend.lobby;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
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
				case "prossima-mano" -> esegui(socketId, tavolo::prossimaMano);
				case "nuova-partita" -> esegui(socketId, tavolo::nuovaPartita);
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
		Map<String, Collegamento> membri = stanze.get(collegamento.codice());
		if (membri == null) {
			return;
		}
		membri.remove(collegamento.sessione(), collegamento);
		if (membri.isEmpty()) {
			stanze.remove(collegamento.codice(), membri);
		}
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

	public record Messaggio(String tipo, String codice, String sessione, Integer gettoni) {
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
