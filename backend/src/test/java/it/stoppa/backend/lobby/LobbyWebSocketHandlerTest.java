package it.stoppa.backend.lobby;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LobbyWebSocketHandlerTest {

	@LocalServerPort
	private int port;

	@Autowired
	private LobbyService lobby;

	@Test
	void dueClientRicevonoLoStatoFiltratoELeMosse() throws Exception {
		IngressoLobby peppa = lobby.crea("Peppa");
		String codice = peppa.stanza().codice();
		IngressoLobby anna = lobby.entra(codice, "Anna");
		lobby.avvia(codice, peppa.sessione());

		Cliente uno = new Cliente(port);
		Cliente due = new Cliente(port);
		try {
			uno.entra(codice, peppa.sessione());
			due.entra(codice, anna.sessione());

			String statoUno = uno.prossimo("\"tipo\":\"stato\"");
			String statoDue = due.prossimo("\"tipo\":\"stato\"");

			assertTrue(statoUno.contains("\"nomeTuo\":\"Peppa\""), statoUno);
			assertTrue(statoDue.contains("\"nomeTuo\":\"Anna\""), statoDue);
			assertTrue(statoUno.contains("\"piatto\":7"), statoUno);
			assertTrue(statoDue.contains("\"piatto\":7"), statoDue);
			assertEquals(1, conta(statoUno, "\"carte\":[{"), "solo la mano di Peppa: " + statoUno);
			assertEquals(1, conta(statoDue, "\"carte\":[{"), "solo la mano di Anna: " + statoDue);

			due.invia("{\"tipo\":\"passa\"}");
			String errore = due.prossimo("\"tipo\":\"errore\"");
			assertTrue(errore.contains("non e' il tuo turno"), errore);

			uno.invia("{\"tipo\":\"passa\"}");
			String dopoUno = uno.prossimo("\"Passi\"");
			String dopoDue = due.prossimo("\"Passi\"");
			assertTrue(dopoUno.contains("\"eventi\""), dopoUno);
			assertTrue(dopoDue.contains("\"eventi\""), dopoDue);
			assertEquals(1, conta(dopoDue, "\"carte\":[{"), "Anna vede solo la sua mano: " + dopoDue);
		} finally {
			uno.close();
			due.close();
		}
	}

	@Test
	void unaMossaViaRestArrivaAncheAiClientWebSocket() throws Exception {
		IngressoLobby peppa = lobby.crea("Peppa");
		String codice = peppa.stanza().codice();
		IngressoLobby anna = lobby.entra(codice, "Anna");
		lobby.avvia(codice, peppa.sessione());

		Cliente uno = new Cliente(port);
		Cliente due = new Cliente(port);
		try {
			uno.entra(codice, peppa.sessione());
			due.entra(codice, anna.sessione());
			uno.prossimo("\"tipo\":\"stato\"");
			due.prossimo("\"tipo\":\"stato\"");

			HttpResponse<String> risposta = post("/api/tavolo/" + codice + "/passa", peppa.sessione());

			assertEquals(200, risposta.statusCode(), risposta.body());
			assertTrue(uno.prossimo("Passi").contains("\"eventi\""));
			assertTrue(due.prossimo("Passi").contains("\"eventi\""));
		} finally {
			uno.close();
			due.close();
		}
	}

	@Test
	void laLobbyArrivaSulSocketSenzaInterrogareIlServer() throws Exception {
		IngressoLobby peppa = lobby.crea("Peppa");
		String codice = peppa.stanza().codice();

		Cliente uno = new Cliente(port);
		try {
			uno.entra(codice, peppa.sessione());
			String iniziale = uno.prossimo("\"tipo\":\"stanza\"");
			assertTrue(iniziale.contains("\"tuoIndice\":0"), iniziale);
			assertTrue(iniziale.contains("\"avviata\":false"), iniziale);

			lobby.entra(codice, "Anna");
			String dopoIngresso = uno.prossimo("\"Anna\"");
			assertTrue(dopoIngresso.contains("\"tipo\":\"stanza\""), dopoIngresso);
			assertTrue(dopoIngresso.contains("\"postiTotali\":8"), dopoIngresso);

			lobby.avvia(codice, peppa.sessione());
			String avviata = uno.prossimo("\"avviata\":true");
			assertTrue(avviata.contains("\"tipo\":\"stanza\""), avviata);
			assertTrue(uno.prossimo("\"tipo\":\"stato\"").contains("\"nomeTuo\":\"Peppa\""));
		} finally {
			uno.close();
		}
	}

	@Test
	void unaSessioneDiUnAltraStanzaNonApreIlSocket() throws Exception {
		IngressoLobby peppa = lobby.crea("Peppa");
		IngressoLobby anna = lobby.crea("Anna");

		Cliente intruso = new Cliente(port);
		try {
			intruso.entra(peppa.stanza().codice(), anna.sessione());

			String errore = intruso.prossimo("\"tipo\":\"errore\"");
			assertTrue(errore.contains("non sei seduto in questa stanza"), errore);
			assertFalse(errore.contains("\"motivo\""), "solo la sessione persa porta un motivo: " + errore);
			assertTrue(intruso.nessunMessaggio(), "la stanza non deve arrivare a un estraneo");

			intruso.invia("{\"tipo\":\"passa\"}");
			assertTrue(intruso.prossimo("prima entra").contains("prima entra"));
		} finally {
			intruso.close();
		}
	}

	@Test
	void unaStanzaSparitaChiedeDiRicominciare() throws Exception {
		IngressoLobby peppa = lobby.crea("Peppa");

		Cliente uno = new Cliente(port);
		try {
			uno.entra("ZZZZ", peppa.sessione());

			String errore = uno.prossimo("\"motivo\":\"ricomincia\"");
			assertTrue(errore.contains("non trovata"), errore);
		} finally {
			uno.close();
		}
	}

	@Test
	void chiPerdeLaSessioneRiceveIlMotivoPerTornareAllaHome() throws Exception {
		IngressoLobby peppa = lobby.crea("Peppa");
		String codice = peppa.stanza().codice();
		IngressoLobby anna = lobby.entra(codice, "Anna");

		Cliente uno = new Cliente(port);
		Cliente due = new Cliente(port);
		try {
			uno.entra(codice, peppa.sessione());
			due.entra(codice, anna.sessione());
			uno.prossimo("\"tipo\":\"stanza\"");
			due.prossimo("\"tipo\":\"stanza\"");

			lobby.togliPosto(codice, anna.stanza().tuoIndice(), peppa.sessione());

			String errore = due.prossimo("\"motivo\":\"ricomincia\"");
			assertTrue(errore.contains("sessione non valida"), errore);
		} finally {
			uno.close();
			due.close();
		}
	}

	@Test
	void allaRiconnessioneSiRisincronizza() throws Exception {
		IngressoLobby peppa = lobby.crea("Peppa");
		String codice = peppa.stanza().codice();
		lobby.avvia(codice, peppa.sessione());

		Cliente primo = new Cliente(port);
		try {
			primo.entra(codice, peppa.sessione());
			assertNotNull(primo.prossimo("\"tipo\":\"stato\""));

			primo.invia("{\"tipo\":\"stato\"}");
			assertTrue(primo.prossimo("\"tipo\":\"stato\"").contains("\"nomeTuo\":\"Peppa\""));
		} finally {
			primo.close();
		}

		Cliente secondo = new Cliente(port);
		try {
			secondo.entra(codice, peppa.sessione());
			String stato = secondo.prossimo("\"tipo\":\"stato\"");
			assertTrue(stato.contains("\"nomeTuo\":\"Peppa\""), stato);
			assertTrue(stato.contains("\"piatto\":6"), stato);
		} finally {
			secondo.close();
		}
	}

	@Test
	void laVocePassaDaUnGiocatoreAllAltroSenzaEsporreLeSessioni() throws Exception {
		IngressoLobby peppa = lobby.crea("Peppa");
		String codice = peppa.stanza().codice();
		IngressoLobby anna = lobby.entra(codice, "Anna");

		Cliente uno = new Cliente(port);
		Cliente due = new Cliente(port);
		try {
			uno.entra(codice, peppa.sessione());
			due.entra(codice, anna.sessione());

			uno.invia("{\"tipo\":\"voce-entra\"}");
			uno.prossimo("\"mio\"");
			due.invia("{\"tipo\":\"voce-entra\"}");

			String rosterUno = uno.prossimo("\"nome\":\"Anna\"");
			String rosterDue = due.prossimo("\"mio\"");
			assertTrue(rosterDue.contains("\"nome\":\"Peppa\""), rosterDue);
			assertFalse(rosterUno.contains(peppa.sessione()), "la sessione non deve uscire: " + rosterUno);
			assertFalse(rosterUno.contains(anna.sessione()), "la sessione non deve uscire: " + rosterUno);

			// il roster che riceve ognuno porta il proprio id in "mio"
			String idPeppa = campo(rosterUno, "mio");
			String idAnna = campo(rosterDue, "mio");
			assertFalse(idPeppa.equals(idAnna));

			uno.invia("{\"tipo\":\"voce\",\"a\":\"" + idAnna
					+ "\",\"dati\":{\"genere\":\"offerta\",\"sdp\":{\"type\":\"offer\",\"sdp\":\"v=0\"}}}");
			String offerta = due.prossimo("\"genere\":\"offerta\"");
			assertTrue(offerta.contains("\"da\":\"" + idPeppa + "\""), offerta);
			assertTrue(offerta.contains("v=0"), offerta);

			due.invia("{\"tipo\":\"voce-muta\",\"muto\":true}");
			String rosterMuto = uno.prossimo("\"muto\":true");
			assertTrue(rosterMuto.contains("Anna"), rosterMuto);
		} finally {
			uno.close();
			due.close();
		}
	}

	private static String campo(String json, String nome) {
		String chiave = "\"" + nome + "\":\"";
		int inizio = json.indexOf(chiave) + chiave.length();
		return json.substring(inizio, json.indexOf('"', inizio));
	}

	private HttpResponse<String> post(String percorso, String sessione) throws Exception {
		HttpRequest richiesta = HttpRequest.newBuilder(URI.create("http://localhost:" + port + percorso))
				.POST(HttpRequest.BodyPublishers.noBody())
				.header("X-Sessione", sessione)
				.build();
		return HttpClient.newHttpClient().send(richiesta, HttpResponse.BodyHandlers.ofString());
	}

	private static int conta(String testo, String parte) {
		int conto = 0;
		int indice = 0;
		while ((indice = testo.indexOf(parte, indice)) >= 0) {
			conto++;
			indice += parte.length();
		}
		return conto;
	}

	private static final class Cliente implements AutoCloseable {

		private final BlockingQueue<String> messaggi = new LinkedBlockingQueue<>();

		private final WebSocket socket;

		private Cliente(int port) throws Exception {
			socket = HttpClient.newHttpClient().newWebSocketBuilder()
					.buildAsync(URI.create("ws://localhost:" + port + "/ws/lobby"), new WebSocket.Listener() {

						private final StringBuilder parziale = new StringBuilder();

						@Override
						public void onOpen(WebSocket webSocket) {
							webSocket.request(1);
						}

						@Override
						public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
							parziale.append(data);
							if (last) {
								messaggi.add(parziale.toString());
								parziale.setLength(0);
							}
							webSocket.request(1);
							return null;
						}
					})
					.get(10, TimeUnit.SECONDS);
		}

		private void entra(String codice, String sessione) {
			invia("{\"tipo\":\"entra\",\"codice\":\"" + codice + "\",\"sessione\":\"" + sessione + "\"}");
		}

		private void invia(String messaggio) {
			socket.sendText(messaggio, true).join();
		}

		/** true se non arriva nessun messaggio entro mezzo secondo. */
		private boolean nessunMessaggio() throws InterruptedException {
			return messaggi.poll(500, TimeUnit.MILLISECONDS) == null;
		}

		/** Il primo messaggio che contiene il testo atteso, scartando gli altri. */
		private String prossimo(String atteso) throws InterruptedException {
			long fine = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
			while (System.nanoTime() < fine) {
				String messaggio = messaggi.poll(200, TimeUnit.MILLISECONDS);
				if (messaggio != null && messaggio.contains(atteso)) {
					return messaggio;
				}
			}
			throw new AssertionError("nessun messaggio con " + atteso);
		}

		@Override
		public void close() {
			if (!socket.isOutputClosed()) {
				socket.sendClose(WebSocket.NORMAL_CLOSURE, "fine test").join();
			}
		}

	}

}
