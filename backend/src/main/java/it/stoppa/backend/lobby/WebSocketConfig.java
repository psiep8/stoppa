package it.stoppa.backend.lobby;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

	private final LobbyWebSocketHandler lobbyHandler;

	private final String[] originiAmmesse;

	public WebSocketConfig(
			LobbyWebSocketHandler lobbyHandler,
			@Value("${stoppa.origini-ammesse}") String[] originiAmmesse) {
		this.lobbyHandler = lobbyHandler;
		this.originiAmmesse = originiAmmesse;
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(lobbyHandler, "/ws/lobby")
				.setAllowedOriginPatterns(originiAmmesse);
	}

}
