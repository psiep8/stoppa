package it.stoppa.backend.lobby;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

	private final LobbyWebSocketHandler lobbyHandler;

	public WebSocketConfig(LobbyWebSocketHandler lobbyHandler) {
		this.lobbyHandler = lobbyHandler;
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(lobbyHandler, "/ws/lobby")
				.setAllowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*");
	}

}
