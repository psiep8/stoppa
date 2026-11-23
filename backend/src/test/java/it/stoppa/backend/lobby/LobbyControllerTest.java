package it.stoppa.backend.lobby;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LobbyControllerTest {

	private static final String SESSIONE = "X-Sessione";

	private final HttpClient client = HttpClient.newHttpClient();

	@LocalServerPort
	private int port;

	@Test
	void creareEntrareEAvviarePassanoDallApi() throws Exception {
		HttpResponse<String> creazione = post("/api/lobby", null, "{\"nome\":\"Peppa\"}");

		assertEquals(200, creazione.statusCode(), creazione.body());
		String codice = estrai(creazione.body(), "\"codice\":\"([A-Z]{4})\"");
		String sessione = estrai(creazione.body(), "\"sessione\":\"([0-9a-f]{32})\"");
		assertTrue(creazione.body().contains("\"tuoIndice\":0"), creazione.body());
		assertTrue(creazione.body().contains("\"avviata\":false"), creazione.body());

		HttpResponse<String> ingresso = post("/api/lobby/" + codice + "/entra", null, "{\"nome\":\"Anna\"}");

		assertEquals(200, ingresso.statusCode(), ingresso.body());
		String sessioneAnna = estrai(ingresso.body(), "\"sessione\":\"([0-9a-f]{32})\"");
		assertTrue(ingresso.body().contains("\"nome\":\"Anna\""), ingresso.body());
		assertTrue(ingresso.body().contains("\"tuoIndice\":6"), ingresso.body());

		HttpResponse<String> avvio = post("/api/lobby/" + codice + "/avvia", sessione, null);

		assertEquals(200, avvio.statusCode(), avvio.body());
		assertTrue(avvio.body().contains("\"nomeTuo\":\"Peppa\""), avvio.body());

		HttpResponse<String> vistaAnna = get("/api/tavolo/" + codice, sessioneAnna);

		assertEquals(200, vistaAnna.statusCode(), vistaAnna.body());
		assertTrue(vistaAnna.body().contains("\"nomeTuo\":\"Anna\""), vistaAnna.body());
		assertTrue(vistaAnna.body().contains("\"tuoIndice\":6"), vistaAnna.body());
	}

	@Test
	void unCodiceSbagliatoNonFaEntrare() throws Exception {
		HttpResponse<String> ingresso = post("/api/lobby/ZZZZ/entra", null, "{\"nome\":\"Anna\"}");

		assertEquals(409, ingresso.statusCode(), ingresso.body());
		assertTrue(ingresso.body().contains("non trovata"), ingresso.body());
	}

	private HttpResponse<String> get(String percorso, String sessione) throws Exception {
		HttpRequest.Builder richiesta = HttpRequest.newBuilder(URI.create("http://localhost:" + port + percorso)).GET();
		if (sessione != null) {
			richiesta.header(SESSIONE, sessione);
		}
		return client.send(richiesta.build(), HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> post(String percorso, String sessione, String corpo) throws Exception {
		HttpRequest.Builder richiesta = HttpRequest
				.newBuilder(URI.create("http://localhost:" + port + percorso))
				.POST(corpo == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(corpo));
		if (sessione != null) {
			richiesta.header(SESSIONE, sessione);
		}
		if (corpo != null) {
			richiesta.header("Content-Type", "application/json");
		}
		return client.send(richiesta.build(), HttpResponse.BodyHandlers.ofString());
	}

	private static String estrai(String json, String espressione) {
		Matcher matcher = Pattern.compile(espressione).matcher(json);
		assertTrue(matcher.find(), "campo assente in: " + json);
		return matcher.group(1);
	}

}
