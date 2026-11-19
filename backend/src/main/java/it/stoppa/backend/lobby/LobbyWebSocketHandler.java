package it.stoppa.backend.lobby;

import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Endpoint di appoggio della lobby.
 * Il protocollo vero (stanze, posti, turni) va definito insieme alle regole della stoppa.
 */
@Component
public class LobbyWebSocketHandler extends TextWebSocketHandler {

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws IOException {
		session.sendMessage(new TextMessage("{\"type\":\"hello\",\"game\":\"stoppa\"}"));
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
		session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
	}

}
