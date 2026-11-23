package it.stoppa.backend.tavolo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import it.stoppa.backend.game.Carta;
import it.stoppa.backend.game.Punteggio;
import it.stoppa.backend.game.Seme;
import it.stoppa.backend.game.Valore;
import it.stoppa.backend.lobby.SessioniService;

class TavoloServiceTest {

	private static final String CODICE = "TEST";

	private final SessioniService sessioni = new SessioniService();

	private final List<StatoTavoloCambiato> annunci = new ArrayList<>();

	private final TavoloService tavolo = new TavoloService(sessioni, evento -> annunci.add((StatoTavoloCambiato) evento));

	private String sessione(String nome, boolean creatore) {
		return sessioni.crea(nome, CODICE, creatore).id();
	}

	/** I posti degli umani passati, completati con i bot fino a quattro giocatori. */
	private List<PostoTavolo> conBot(String... umani) {
		List<PostoTavolo> posti = new ArrayList<>();
		for (int i = 0; i < umani.length; i++) {
			posti.add(new PostoTavolo(umani[i], i == 0 ? "Tu" : "Giocatore " + i));
		}
		for (int i = 0; i < 4 - umani.length; i++) {
			posti.add(new PostoTavolo(null, "Bot " + (i + 1)));
		}
		return posti;
	}

	@Test
	void ilTavoloParteConQuattroGiocatoriEMostraSoloLeTueCarte() {
		String creatore = sessione("Tu", true);
		tavolo.crea(CODICE, conBot(creatore));

		StatoTavolo stato = tavolo.stato(CODICE, creatore);

		assertEquals(4, stato.giocatori().size());
		assertEquals("Tu", stato.nomeTuo());
		GiocatoreTavolo io = stato.giocatori().get(stato.tuoIndice());
		assertFalse(io.carte().isEmpty());
		assertTrue(io.carte().size() <= 3);
		stato.giocatori().stream().filter(GiocatoreTavolo::bot).forEach(bot -> assertTrue(bot.carte().isEmpty(),
				"le carte di " + bot.nome() + " non devono uscire dal server"));
		assertEquals(4, stato.mano().piatto());
		assertTrue(stato.giocatori().stream().allMatch(giocatore -> giocatore.fiches() == 9));
	}

	@Test
	void ilVerbaleRiportaFichesGiocateERimaste() {
		String creatore = sessione("Tu", true);
		tavolo.crea(CODICE, conBot(creatore));

		String verbale = tavolo.verbale(CODICE, creatore);

		assertTrue(verbale.startsWith("<!doctype html>"), verbale);
		assertTrue(verbale.contains("<th>Iniziali</th>"), verbale);
		assertTrue(verbale.contains("<th>Finali</th>"), verbale);
		assertTrue(verbale.contains("<th>Cambi</th>"), verbale);
		assertTrue(verbale.contains("<td>Bot 1</td>"), verbale);
		// ognuno parte da dieci gettoni
		assertTrue(verbale.contains("<td>Tu</td><td>10</td>"), verbale);
		assertFalse(verbale.contains("Classifica"), verbale);
	}

	@Test
	void ilVerbaleNonSiPrendeSenzaSessione() {
		assertThrows(IllegalStateException.class, () -> tavolo.verbale(CODICE, null));
	}

	@Test
	void ogniSessioneVedeSoloLeProprieCarte() {
		String peppa = sessione("Peppa", true);
		String anna = sessione("Anna", false);
		tavolo.crea(CODICE, conBot(peppa, anna));

		StatoTavolo vistaPeppa = tavolo.stato(CODICE, peppa);
		StatoTavolo vistaAnna = tavolo.stato(CODICE, anna);

		assertEquals(0, vistaPeppa.tuoIndice());
		assertEquals(1, vistaAnna.tuoIndice());
		assertFalse(vistaPeppa.giocatori().get(0).carte().isEmpty());
		assertTrue(vistaPeppa.giocatori().get(1).carte().isEmpty());
		assertFalse(vistaAnna.giocatori().get(1).carte().isEmpty());
		assertTrue(vistaAnna.giocatori().get(0).carte().isEmpty());
	}

	@Test
	void ogniMossaAvvisaLaStanza() {
		String creatore = sessione("Tu", true);
		tavolo.crea(CODICE, conBot(creatore));

		int prima = annunci.size();
		tavolo.passa(CODICE, creatore);

		assertEquals(prima + 1, annunci.size(), "una mossa deve avvisare la stanza una volta sola");
		assertEquals(CODICE, annunci.getLast().codice());
	}

	@Test
	void unaSessioneNonPuoOccupareDuePosti() {
		String creatore = sessione("Tu", true);
		List<PostoTavolo> doppioni = List.of(new PostoTavolo(creatore, "Tu"), new PostoTavolo(creatore, "Doppione"),
				new PostoTavolo(null, "Bot 1"), new PostoTavolo(null, "Bot 2"));

		assertThrows(IllegalArgumentException.class, () -> tavolo.crea(CODICE, doppioni));
	}

	@Test
	void ilCodiceDellaStanzaNonCambiaConLeMaiuscole() {
		String creatore = sessione("Tu", true);
		tavolo.crea(CODICE, conBot(creatore));

		assertEquals(0, tavolo.stato("test", creatore).tuoIndice());
	}

	@Test
	void unaSessioneSenzaPostoNonVedeLeCarte() {
		String creatore = sessione("Tu", true);
		tavolo.crea(CODICE, conBot(creatore));
		String senzaPosto = sessione("Senza posto", false);

		IllegalStateException errore = assertThrows(IllegalStateException.class,
				() -> tavolo.stato(CODICE, senzaPosto));

		assertTrue(errore.getMessage().contains("posto"), errore.getMessage());
	}

	@Test
	void unaSessioneDiUnAltraStanzaNonVedeIlTavolo() {
		String creatore = sessione("Tu", true);
		tavolo.crea(CODICE, conBot(creatore));
		String intruso = sessioni.crea("Intruso", "ALTRA", false).id();

		assertThrows(IllegalStateException.class, () -> tavolo.stato(CODICE, intruso));
		assertEquals(0, tavolo.stato(CODICE, creatore).tuoIndice());
	}

	@Test
	void nonSiPuoVedereSeNessunoHaPuntato() {
		String creatore = sessione("Tu", true);
		tavolo.crea(CODICE, conBot(creatore));

		StatoTavolo stato = tavolo.stato(CODICE, creatore);

		assertTrue(stato.toccaATe(), "il cartaro iniziale e' il primo posto");
		assertEquals(0, stato.mano().puntataMassima());
		assertThrows(IllegalStateException.class, () -> tavolo.vedo(CODICE, creatore));
	}

	@Test
	void unaMossaFuoriTurnoNonPassa() {
		String peppa = sessione("Peppa", true);
		String anna = sessione("Anna", false);
		tavolo.crea(CODICE, conBot(peppa, anna));

		assertThrows(IllegalStateException.class, () -> tavolo.passa(CODICE, anna));
	}

	@Test
	void unaPartitaSiGiocaDallInizioAllaFine() {
		String creatore = sessione("Tu", true);
		tavolo.crea(CODICE, conBot(creatore));

		StatoTavolo stato = tavolo.stato(CODICE, creatore);
		int guardia = 0;
		while (!stato.finita() && guardia++ < 400) {
			if (stato.mano().chiusa()) {
				stato = tavolo.prossimaMano(CODICE, creatore);
				continue;
			}
			if (stato.mano().inDichiarazione()) {
				int punti = Punteggio.calcola(stato.giocatori().get(stato.tuoIndice()).carte().stream()
						.map(carta -> new Carta(Seme.valueOf(carta.seme()), Valore.valueOf(carta.valore())))
						.toList());
				if (stato.puoiRispondere()) {
					// lo stato dice di poter contestare solo se il punto in mano basta
					assertEquals(punti >= stato.mano().dichiarazione(), stato.puoiContestare(),
							"punti " + punti + " contro i " + stato.mano().dichiarazione() + " dichiarati");
				}
				if (stato.puoiDichiarare()) {
					// dire il vero: dopo tre bluff il server accetta solo la verita'
					stato = tavolo.dichiara(CODICE, creatore, punti);
				} else if (stato.puoiRispondere()) {
					stato = stato.puoiContestare() ? tavolo.contesta(CODICE, creatore)
							: tavolo.accetta(CODICE, creatore);
				}
				continue;
			}
			assertTrue(stato.toccaATe(), "dopo ogni mossa deve toccare al giocatore umano");
			stato = stato.mano().puntataMassima() == 0 ? tavolo.passa(CODICE, creatore)
					: tavolo.vedo(CODICE, creatore);
		}

		assertTrue(stato.finita(), "la partita non si e' conclusa");
		String verbale = tavolo.verbale(CODICE, creatore);
		assertTrue(verbale.contains("partita conclusa"), verbale);
		assertTrue(verbale.contains("<th>Cambi</th>"), verbale);
		assertFalse(verbale.contains("Classifica"), verbale);
	}

	@Test
	void unaNuovaPartitaAzzeraIlTavolo() {
		String creatore = sessione("Tu", true);
		tavolo.crea(CODICE, conBot(creatore));
		tavolo.passa(CODICE, creatore);

		StatoTavolo nuova = tavolo.nuovaPartita(CODICE, creatore);

		assertEquals(1, nuova.mano().numero());
		assertEquals(4, nuova.mano().piatto());
		assertTrue(nuova.giocatori().stream().allMatch(giocatore -> giocatore.fiches() == 9));
		assertFalse(nuova.finita());
	}

	@Test
	void ilRiepilogoESoloDiChiCreaLaLobby() {
		String peppa = sessione("Peppa", true);
		String anna = sessione("Anna", false);
		tavolo.crea(CODICE, conBot(peppa, anna));

		assertThrows(IllegalStateException.class, () -> tavolo.riepilogo(CODICE, anna));
		assertEquals(4, tavolo.riepilogo(CODICE, peppa).piatto());
		assertEquals(9, tavolo.riepilogo(CODICE, peppa).giocatori().getFirst().fiches());
	}

}
