package it.stoppa.backend.lobby;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LobbyWebSocketHandlerTest {

	private final HttpClient client = HttpClient.newHttpClient();

	@LocalServerPort
	private int port;

	@Test
	void handshakeSendsHelloAndAnswersPingWithPong() throws Exception {
		BlockingQueue<String> received = new LinkedBlockingQueue<>();

		WebSocket socket = client.newWebSocketBuilder()
				.buildAsync(URI.create("ws://localhost:" + port + "/ws/lobby"), new WebSocket.Listener() {
					@Override
					public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
						received.add(data.toString());
						webSocket.request(1);
						return null;
					}
				})
				.get(10, TimeUnit.SECONDS);

		// buildAsync(...).get() completa solo se l'upgrade HTTP 101 e' andato a buon fine
		String hello = received.poll(10, TimeUnit.SECONDS);
		assertNotNull(hello, "nessun messaggio di benvenuto ricevuto");
		assertTrue(hello.contains("\"type\":\"hello\""), hello);
		assertTrue(hello.contains("\"game\":\"stoppa\""), hello);

		socket.sendText("ping", true);

		String pong = received.poll(10, TimeUnit.SECONDS);
		assertNotNull(pong, "nessuna risposta al ping");
		assertTrue(pong.contains("\"type\":\"pong\""), pong);

		socket.sendClose(WebSocket.NORMAL_CLOSURE, "test completato").join();
	}

}
