package it.stoppa.backend.tavolo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import it.stoppa.backend.lobby.IngressoLobby;
import it.stoppa.backend.lobby.LobbyService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TavoloControllerTest {

	private static final String SESSIONE = "X-Sessione";

	private final HttpClient client = HttpClient.newHttpClient();

	@LocalServerPort
	private int port;

	@Autowired
	private LobbyService lobby;

	@Test
	void loStatoRispondeSoloConLaSessioneGiusta() throws Exception {
		IngressoLobby ingresso = lobby.crea("Peppa");
		lobby.avvia(ingresso.stanza().codice(), ingresso.sessione());

		HttpResponse<String> conSessione = get("/api/tavolo/" + ingresso.stanza().codice(), ingresso.sessione());

		assertEquals(200, conSessione.statusCode(), conSessione.body());
		assertTrue(conSessione.body().contains("\"nomeTuo\":\"Peppa\""), conSessione.body());
		assertEquals(1, conSessione.body().split("\"carte\":\\[\\{").length - 1,
				"deve uscire solo la mano di chi chiede: " + conSessione.body());

		HttpResponse<String> senzaSessione = get("/api/tavolo/" + ingresso.stanza().codice(), null);

		assertEquals(409, senzaSessione.statusCode(), senzaSessione.body());
		assertTrue(senzaSessione.body().contains(SESSIONE), senzaSessione.body());
	}

	@Test
	void ilRiepilogoEperChiCreaLaLobby() throws Exception {
		IngressoLobby ingresso = lobby.crea("Peppa");
		lobby.avvia(ingresso.stanza().codice(), ingresso.sessione());

		HttpResponse<String> riepilogo = get("/api/tavolo/" + ingresso.stanza().codice() + "/riepilogo",
				ingresso.sessione());

		assertEquals(200, riepilogo.statusCode(), riepilogo.body());
		assertTrue(riepilogo.body().contains("\"piatto\":6"), riepilogo.body());
		assertTrue(riepilogo.body().contains("\"nome\":\"Peppa\""), riepilogo.body());
	}

	@Test
	void unaMossaFuoriTurnoNonPassa() throws Exception {
		IngressoLobby creatore = lobby.crea("Peppa");
		String codice = creatore.stanza().codice();
		IngressoLobby ospite = lobby.entra(codice, "Anna");
		lobby.avvia(codice, creatore.sessione());

		HttpResponse<String> risposta = post("/api/tavolo/" + codice + "/passa", ospite.sessione());

		assertEquals(409, risposta.statusCode(), risposta.body());
		assertTrue(risposta.body().contains("non e' il tuo turno"), risposta.body());
	}

	private HttpResponse<String> get(String percorso, String sessione) throws Exception {
		HttpRequest.Builder richiesta = HttpRequest.newBuilder(URI.create("http://localhost:" + port + percorso)).GET();
		if (sessione != null) {
			richiesta.header(SESSIONE, sessione);
		}
		return client.send(richiesta.build(), HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> post(String percorso, String sessione) throws Exception {
		HttpRequest.Builder richiesta = HttpRequest
				.newBuilder(URI.create("http://localhost:" + port + percorso))
				.POST(HttpRequest.BodyPublishers.noBody());
		if (sessione != null) {
			richiesta.header(SESSIONE, sessione);
		}
		return client.send(richiesta.build(), HttpResponse.BodyHandlers.ofString());
	}

}
