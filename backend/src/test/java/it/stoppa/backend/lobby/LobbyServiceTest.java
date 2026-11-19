package it.stoppa.backend.lobby;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.stoppa.backend.tavolo.StatoTavolo;
import it.stoppa.backend.tavolo.TavoloService;

class LobbyServiceTest {

	private final LobbyService lobby = new LobbyService(new TavoloService());

	@Test
	void laStanzaParteConTeEcinqueBot() {
		StanzaLobby stanza = lobby.crea("Peppa");

		assertEquals(4, stanza.codice().length());
		assertEquals(6, stanza.posti().size());
		assertEquals("Peppa", stanza.posti().getFirst().nome());
		assertFalse(stanza.posti().getFirst().bot());
		assertTrue(stanza.posti().stream().skip(1).allMatch(PostoLobby::bot));
	}

	@Test
	void ilTuoPostoNonSiToglie() {
		StanzaLobby stanza = lobby.crea("Peppa");
		assertThrows(IllegalStateException.class, () -> lobby.togliPosto(stanza.codice(), 0));
	}

	@Test
	void iPostiSiRinumeranoDopoUnUscita() {
		StanzaLobby stanza = lobby.crea("Peppa");

		StanzaLobby dopo = lobby.togliPosto(stanza.codice(), 2);

		assertEquals(5, dopo.posti().size());
		for (int i = 0; i < dopo.posti().size(); i++) {
			assertEquals(i, dopo.posti().get(i).indice());
		}
	}

	@Test
	void siArrivaAOttoeNonOltre() {
		StanzaLobby stanza = lobby.crea("Peppa");
		lobby.aggiungiBot(stanza.codice());
		StanzaLobby piena = lobby.aggiungiBot(stanza.codice());

		assertEquals(8, piena.posti().size());
		assertThrows(IllegalStateException.class, () -> lobby.aggiungiBot(piena.codice()));
	}

	@Test
	void avviarePrendeIPostiDellaStanza() {
		StanzaLobby stanza = lobby.crea("Peppa");

		StatoTavolo tavolo = lobby.avvia(stanza.codice());

		assertEquals(6, tavolo.giocatori().size());
		assertEquals("Peppa", tavolo.giocatori().getFirst().nome());
		assertTrue(tavolo.giocatori().stream().skip(1).allMatch(giocatore -> giocatore.nome().startsWith("Bot")));
		assertEquals(6, tavolo.mano().piatto());
	}

	@Test
	void unaStanzaCheNonEsisteNonSiTrova() {
		assertThrows(IllegalStateException.class, () -> lobby.stanza("ZZZZ"));
	}

	@Test
	void ilCodiceNonCambiaTraLeChiamate() {
		StanzaLobby stanza = lobby.crea("Peppa");
		List<PostoLobby> posti = lobby.stanza(stanza.codice()).posti();

		assertEquals(stanza.posti(), posti);
	}

}
