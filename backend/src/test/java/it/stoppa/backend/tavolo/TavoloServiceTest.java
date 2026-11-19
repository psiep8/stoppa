package it.stoppa.backend.tavolo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TavoloServiceTest {

	@Test
	void ilTavoloParteConQuattroGiocatoriEMostraSoloLeTueCarte() {
		StatoTavolo stato = new TavoloService().stato();

		assertEquals(4, stato.giocatori().size());
		assertEquals(TavoloService.NOME_UMANO, stato.giocatori().get(stato.tuoIndice()).nome());
		GiocatoreTavolo io = stato.giocatori().get(stato.tuoIndice());
		assertFalse(io.carte().isEmpty());
		assertTrue(io.carte().size() <= 3);
		stato.giocatori().stream().filter(GiocatoreTavolo::bot).forEach(bot -> assertTrue(bot.carte().isEmpty(),
				"le carte di " + bot.nome() + " non devono uscire dal server"));
	}

	@Test
	void leCarteArrivanoAlBrowserConIlFileGiusto() {
		StatoTavolo stato = new TavoloService().stato();

		stato.giocatori().get(stato.tuoIndice()).carte().forEach(carta -> {
			assertTrue(carta.file().matches("(denari|coppe|spade|bastoni)-(asso|0[2-7]|fante|cavallo|re)"),
					"file inatteso: " + carta.file());
			assertTrue(carta.punti() >= 10);
		});
	}

	@Test
	void nonSiPuoVedereSeNessunoHaPuntato() {
		TavoloService tavolo = new TavoloService();
		StatoTavolo stato = tavolo.stato();

		assertTrue(stato.toccaATe(), "il cartaro iniziale e' il giocatore umano");
		assertEquals(0, stato.mano().puntataMassima());
		assertThrows(IllegalStateException.class, tavolo::vedo);
	}

	@Test
	void unaPartitaSiGiocaDallInizioAllaFine() {
		TavoloService tavolo = new TavoloService();
		StatoTavolo stato = tavolo.stato();

		int guardia = 0;
		while (!stato.finita() && guardia++ < 400) {
			if (stato.mano().chiusa()) {
				stato = tavolo.prossimaMano();
				continue;
			}
			assertTrue(stato.toccaATe(), "dopo ogni mossa deve toccare al giocatore umano");
			stato = stato.mano().puntataMassima() == 0 ? tavolo.passa() : tavolo.vedo();
		}

		assertTrue(stato.finita(), "la partita non si e' conclusa");
		assertEquals(4, stato.classifica().size());
		assertEquals(1, stato.classifica().getFirst().posizione());
	}

	@Test
	void unaNuovaPartitaAzzeraIlTavolo() {
		TavoloService tavolo = new TavoloService();
		tavolo.stato();
		tavolo.passa();

		StatoTavolo stato = tavolo.nuovaPartita();

		assertEquals(1, stato.mano().numero());
		assertTrue(stato.giocatori().stream().allMatch(giocatore -> giocatore.fiches() == 10));
		assertFalse(stato.finita());
	}

}
