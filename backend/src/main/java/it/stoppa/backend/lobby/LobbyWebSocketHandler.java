package it.stoppa.backend.lobby;

import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Endpoint di appoggio della lobby.
 * Il protocollo vero (stanze, posti, turni di gioco) va definito insieme alle regole della stoppa.
 */
@Component
public class LobbyWebSocketHandler extends TextWebSocketHandler {

	private static final String HELLO = "{\"type\":\"hello\",\"game\":\"stoppa\",\"message\":\"lobby connessa\"}";
	private static final String PONG = "{\"type\":\"pong\"}";

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws IOException {
		session.sendMessage(new TextMessage(HELLO));
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
		session.sendMessage(new TextMessage(PONG));
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		// nessuno stato da pulire finche' la lobby non gestisce le stanze
	}

}
