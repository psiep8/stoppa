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

	private final SessioniService sessioni = new SessioniService();

	private final LobbyService lobby = new LobbyService(sessioni, new TavoloService(sessioni, evento -> {
	}), evento -> {
	});

	@Test
	void laStanzaParteConTeEcinqueBot() {
		IngressoLobby ingresso = lobby.crea("Peppa");
		StanzaLobby stanza = ingresso.stanza();

		assertEquals(4, stanza.codice().length());
		assertEquals(32, ingresso.sessione().length());
		assertEquals(6, stanza.posti().size());
		assertEquals("Peppa", stanza.posti().getFirst().nome());
		assertFalse(stanza.posti().getFirst().bot());
		assertEquals(0, stanza.tuoIndice());
		assertFalse(stanza.avviata());
		assertTrue(stanza.posti().stream().skip(1).allMatch(PostoLobby::bot));
	}

	@Test
	void ilTuoPostoNonSiToglie() {
		IngressoLobby ingresso = lobby.crea("Peppa");

		assertThrows(IllegalStateException.class,
				() -> lobby.togliPosto(ingresso.stanza().codice(), 0, ingresso.sessione()));
	}

	@Test
	void iPostiSiRinumeranoDopoUnUscitaESeguonoLeSessioni() {
		IngressoLobby creatore = lobby.crea("Peppa");
		String codice = creatore.stanza().codice();
		IngressoLobby anna = lobby.entra(codice, "Anna");
		IngressoLobby luca = lobby.entra(codice, "Luca");

		StanzaLobby dopo = lobby.togliPosto(codice, 1, creatore.sessione());

		assertEquals(7, dopo.posti().size());
		for (int i = 0; i < dopo.posti().size(); i++) {
			assertEquals(i, dopo.posti().get(i).indice());
		}
		assertEquals(5, lobby.stanza(codice, anna.sessione()).tuoIndice());
		assertEquals(6, lobby.stanza(codice, luca.sessione()).tuoIndice());
	}

	@Test
	void chiVieneToltoDallaStanzaPerdeLaSessione() {
		IngressoLobby creatore = lobby.crea("Peppa");
		String codice = creatore.stanza().codice();
		IngressoLobby anna = lobby.entra(codice, "Anna");

		lobby.togliPosto(codice, anna.stanza().tuoIndice(), creatore.sessione());

		assertThrows(IllegalStateException.class, () -> lobby.stanza(codice, anna.sessione()));
	}

	@Test
	void siArrivaAOttoeNonOltre() {
		IngressoLobby ingresso = lobby.crea("Peppa");
		String codice = ingresso.stanza().codice();
		lobby.aggiungiBot(codice, ingresso.sessione());
		StanzaLobby piena = lobby.aggiungiBot(codice, ingresso.sessione());

		assertEquals(8, piena.posti().size());
		assertThrows(IllegalStateException.class, () -> lobby.aggiungiBot(codice, ingresso.sessione()));
		assertThrows(IllegalStateException.class, () -> lobby.entra(codice, "Tardi"));
	}

	@Test
	void avviarePrendeIPostiDellaStanza() {
		IngressoLobby ingresso = lobby.crea("Peppa");

		StatoTavolo tavolo = lobby.avvia(ingresso.stanza().codice(), ingresso.sessione());

		assertEquals(6, tavolo.giocatori().size());
		assertEquals("Peppa", tavolo.giocatori().getFirst().nome());
		assertTrue(tavolo.giocatori().stream().skip(1).allMatch(giocatore -> giocatore.nome().startsWith("Bot")));
		assertEquals(6, tavolo.mano().piatto());
		assertTrue(tavolo.giocatori().stream().allMatch(giocatore -> giocatore.fiches() == 9));
	}

	@Test
	void chiEntraRiceveIlProprioPostoEUnPostoAlTavolo() {
		IngressoLobby creatore = lobby.crea("Peppa");
		IngressoLobby anna = lobby.entra(creatore.stanza().codice(), "Anna");

		assertEquals(6, anna.stanza().tuoIndice());
		assertEquals("Anna", anna.stanza().posti().get(6).nome());

		StatoTavolo tavolo = lobby.avvia(creatore.stanza().codice(), creatore.sessione());

		assertEquals(7, tavolo.giocatori().size());
		assertEquals("Anna", tavolo.giocatori().get(6).nome());
		assertFalse(tavolo.giocatori().get(6).bot());
	}

	@Test
	void soloIlCreatorePuoAggiungereBotToglierePostiEAvviare() {
		IngressoLobby creatore = lobby.crea("Peppa");
		String codice = creatore.stanza().codice();
		IngressoLobby anna = lobby.entra(codice, "Anna");

		assertThrows(IllegalStateException.class, () -> lobby.aggiungiBot(codice, anna.sessione()));
		assertThrows(IllegalStateException.class, () -> lobby.togliPosto(codice, 1, anna.sessione()));
		assertThrows(IllegalStateException.class, () -> lobby.avvia(codice, anna.sessione()));
	}

	@Test
	void nonSiEntraInUnaStanzaAvviata() {
		IngressoLobby ingresso = lobby.crea("Peppa");
		lobby.avvia(ingresso.stanza().codice(), ingresso.sessione());

		assertThrows(IllegalStateException.class, () -> lobby.entra(ingresso.stanza().codice(), "Tardi"));
	}

	@Test
	void unaSessioneDiUnAltraStanzaNonVale() {
		IngressoLobby prima = lobby.crea("Peppa");
		IngressoLobby altra = lobby.crea("Anna");

		assertThrows(IllegalStateException.class,
				() -> lobby.aggiungiBot(prima.stanza().codice(), altra.sessione()));
	}

	@Test
	void senzaSessioneLaStanzaNonSaChiSei() {
		IngressoLobby ingresso = lobby.crea("Peppa");

		StanzaLobby vista = lobby.stanza(ingresso.stanza().codice(), null);

		assertEquals(-1, vista.tuoIndice());
		assertEquals(6, vista.posti().size());
	}

	@Test
	void unaStanzaCheNonEsisteNonSiTrova() {
		assertThrows(IllegalStateException.class, () -> lobby.stanza("ZZZZ", null));
	}

	@Test
	void unaStanzaConPinVuoleLaSecondaChiave() {
		IngressoLobby ingresso = lobby.crea("Peppa", "4821");

		assertTrue(ingresso.stanza().conPin());
		assertThrows(IllegalStateException.class, () -> lobby.entra(ingresso.stanza().codice(), "Anna"));
		assertThrows(IllegalStateException.class, () -> lobby.entra(ingresso.stanza().codice(), "Anna", "0000"));

		IngressoLobby anna = lobby.entra(ingresso.stanza().codice(), "Anna", "4821");

		assertEquals(6, anna.stanza().tuoIndice());
		assertTrue(anna.stanza().conPin());
	}

	@Test
	void ilPinDeveAvereQuattroCifre() {
		assertThrows(IllegalStateException.class, () -> lobby.crea("Peppa", "12"));
	}

	@Test
	void unaLobbyChiusaNonAccoglieChiArrivaDaFuori() {
		IngressoLobby creatore = lobby.crea("Peppa");
		String codice = creatore.stanza().codice();
		IngressoLobby anna = lobby.entra(codice, "Anna");

		StanzaLobby chiusa = lobby.impostaChiusa(codice, true, creatore.sessione());

		assertTrue(chiusa.chiusa());
		assertThrows(IllegalStateException.class, () -> lobby.entra(codice, "Luca"));
		assertEquals(7, lobby.stanza(codice, anna.sessione()).posti().size());

		StanzaLobby riaperta = lobby.impostaChiusa(codice, false, creatore.sessione());

		assertFalse(riaperta.chiusa());
		assertEquals(8, lobby.entra(codice, "Luca").stanza().posti().size());
	}

	@Test
	void soloIlCreatoreChiudeLaLobby() {
		IngressoLobby creatore = lobby.crea("Peppa");
		IngressoLobby anna = lobby.entra(creatore.stanza().codice(), "Anna");

		assertThrows(IllegalStateException.class,
				() -> lobby.impostaChiusa(creatore.stanza().codice(), true, anna.sessione()));
	}

	@Test
	void laStanzaDiceAChiGuardaSeHaApertoLui() {
		IngressoLobby creatore = lobby.crea("Peppa");
		IngressoLobby anna = lobby.entra(creatore.stanza().codice(), "Anna");

		assertTrue(lobby.stanza(creatore.stanza().codice(), creatore.sessione()).creatore());
		assertFalse(lobby.stanza(creatore.stanza().codice(), anna.sessione()).creatore());
	}

	@Test
	void ilCodiceNonCambiaTraLeChiamate() {
		IngressoLobby ingresso = lobby.crea("Peppa");
		List<PostoLobby> posti = lobby.stanza(ingresso.stanza().codice(), ingresso.sessione()).posti();

		assertEquals(ingresso.stanza().posti(), posti);
	}

}
