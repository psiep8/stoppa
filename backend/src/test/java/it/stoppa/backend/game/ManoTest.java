package it.stoppa.backend.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ManoTest {

	private final Giocatore a = new Giocatore("A");

	private final Giocatore b = new Giocatore("B");

	private final Giocatore c = new Giocatore("C");

	private final Giocatore d = new Giocatore("D");

	private final List<Giocatore> giocatori = List.of(a, b, c, d);

	private Mano mano() {
		return new Mano(1, 0, giocatori, List.of(
				List.of(new Carta(Seme.DENARI, Valore.SETTE), new Carta(Seme.DENARI, Valore.SEI),
						new Carta(Seme.DENARI, Valore.ASSO)),
				List.of(new Carta(Seme.COPPE, Valore.RE), new Carta(Seme.COPPE, Valore.CINQUE)),
				List.of(new Carta(Seme.SPADE, Valore.CINQUE)),
				List.of(new Carta(Seme.BASTONI, Valore.QUATTRO))));
	}

	@Test
	void seTuttiPassanoLaManoNonHaVincitori() {
		Mano mano = mano();
		for (int i = 0; i < 4; i++) {
			mano.passa();
		}

		assertTrue(mano.chiusa());
		assertTrue(mano.senzaPuntate());
		assertEquals(List.of(), mano.vincitori());
		assertEquals(0, mano.totalePuntate());
	}

	@Test
	void nonSiPuoVedereSenzaPuntate() {
		Mano mano = mano();
		assertThrows(IllegalStateException.class, mano::vedo);
	}

	@Test
	void laPuntataStaTraUnoEDieciGettoni() {
		assertThrows(IllegalArgumentException.class, () -> mano().punta(0));
		assertThrows(IllegalArgumentException.class, () -> mano().punta(11));
	}

	@Test
	void chiRestaSoloPrendeLaPuntataSenzaConfronto() {
		Mano mano = mano();
		mano.punta(3);
		mano.passa();
		mano.passa();
		mano.passa();

		assertTrue(mano.chiusa());
		assertEquals(List.of(0), mano.vincitori());
		assertEquals(3, mano.totalePuntate());
		assertEquals(7, a.fiches());
	}

	@Test
	void delVincitoreSiMostranoSoloLeCarteCheFannoIlPunto() {
		Mano mano = new Mano(1, 0, giocatori, List.of(
				List.of(new Carta(Seme.DENARI, Valore.SETTE), new Carta(Seme.DENARI, Valore.SEI),
						new Carta(Seme.COPPE, Valore.RE)),
				List.of(new Carta(Seme.COPPE, Valore.CINQUE)),
				List.of(new Carta(Seme.SPADE, Valore.CINQUE)),
				List.of(new Carta(Seme.BASTONI, Valore.QUATTRO))));
		mano.punta(2);
		mano.passa();
		mano.passa();
		mano.passa();

		assertEquals(List.of(new Carta(Seme.DENARI, Valore.SETTE), new Carta(Seme.DENARI, Valore.SEI)),
				mano.carteVincenti());
		assertEquals(39, Punteggio.calcola(mano.carteVincenti()));
	}

	@Test
	void quandoTuttiPareggianoSiApreLaDichiarazione() {
		Mano mano = mano();
		mano.punta(3);
		mano.vedo();
		mano.vedo();
		mano.vedo();

		assertTrue(mano.inDichiarazione());
		assertFalse(mano.chiusa());

		mano.dichiara(mano.dichiarante(), 20);
		mano.contesta((mano.dichiarante() + 1) % 4);

		assertTrue(mano.chiusa());
		assertEquals(12, mano.totalePuntate());
		assertEquals(List.of(0), mano.vincitori());
		assertEquals(3, mano.carteVincenti().size());
		assertEquals(7, a.fiches());
		assertEquals(7, b.fiches());
	}

	@Test
	void nonSiPuoPareggiarePuntando() {
		Mano mano = mano();
		mano.punta(3);

		assertThrows(IllegalArgumentException.class, () -> mano.punta(3));
	}

	@Test
	void aParitaDiPuntoVinceChiHaPuntatoPerPrimo() {
		Mano mano = new Mano(1, 0, giocatori, List.of(
				List.of(new Carta(Seme.DENARI, Valore.SETTE)),
				List.of(new Carta(Seme.COPPE, Valore.SETTE)),
				List.of(new Carta(Seme.SPADE, Valore.QUATTRO)),
				List.of(new Carta(Seme.BASTONI, Valore.TRE))));
		mano.punta(2);
		mano.punta(3);
		mano.vedo();
		mano.vedo();
		mano.vedo();

		assertTrue(mano.inDichiarazione());
		mano.dichiara(mano.dichiarante(), 21);
		mano.contesta(0);

		assertTrue(mano.chiusa());
		assertEquals(List.of(0), mano.vincitori());
		assertEquals("buono-di-mano", mano.esitoDichiarazione());
	}

	@Test
	void chiDiceLaVeritaEAccettatoDaTuttiVince() {
		Mano mano = mano();
		mano.punta(3);
		mano.vedo();
		mano.vedo();
		mano.vedo();

		int dichiarante = mano.dichiarante();
		mano.dichiara(dichiarante, Punteggio.calcola(mano.carteDi(dichiarante)));
		accettanoTutti(mano);

		assertTrue(mano.chiusa());
		assertEquals(List.of(dichiarante), mano.vincitori());
		assertEquals("buono", mano.esitoDichiarazione());
	}

	@Test
	void seTuttiAccettanoUnBluffSiParlaDiNuovo() {
		Mano mano = mano();
		mano.punta(3);
		mano.vedo();
		mano.vedo();
		mano.vedo();

		int dichiarante = mano.dichiarante();
		mano.dichiara(dichiarante, 20);
		accettanoTutti(mano);

		assertFalse(mano.chiusa(), "un bluff accettato non chiude la mano");
		assertTrue(mano.inDichiarazione());
		assertEquals(0, mano.dichiarazione());
		assertTrue(mano.accettato().isEmpty(), "le risposte si azzerano: si risponde di nuovo");
		assertEquals(dichiarante, mano.dichiarante(), "parla sempre chi ha aperto");
		assertEquals(2, mano.bluffRimasti());
	}

	@Test
	void dopoTreBluffAccettatiBisognaDireLaVerita() {
		Mano mano = mano();
		mano.punta(3);
		mano.vedo();
		mano.vedo();
		mano.vedo();

		int dichiarante = mano.dichiarante();
		for (int giro = 1; giro <= Mano.TETTO_BLUFF; giro++) {
			mano.dichiara(dichiarante, 20);
			accettanoTutti(mano);
			assertEquals(Mano.TETTO_BLUFF - giro, mano.bluffRimasti());
		}

		assertTrue(mano.inDichiarazione(), "dopo tre bluff la mano e' ancora aperta");
		assertThrows(IllegalStateException.class, () -> mano.dichiara(dichiarante, 20));

		mano.dichiara(dichiarante, Punteggio.calcola(mano.carteDi(dichiarante)));
		accettanoTutti(mano);

		assertTrue(mano.chiusa());
		assertEquals(List.of(dichiarante), mano.vincitori());
		assertEquals("buono", mano.esitoDichiarazione());
	}

	private static void accettanoTutti(Mano mano) {
		for (int giocatore : mano.attivi()) {
			if (giocatore != mano.dichiarante()) {
				mano.accetta(giocatore);
			}
		}
	}

	@Test
	void chiBluffaAlDiSottoDelPuntoAltroVieneSmentito() {
		Mano mano = new Mano(1, 0, giocatori, List.of(
				List.of(new Carta(Seme.DENARI, Valore.DUE)),
				List.of(new Carta(Seme.COPPE, Valore.SETTE)),
				List.of(new Carta(Seme.SPADE, Valore.QUATTRO)),
				List.of(new Carta(Seme.BASTONI, Valore.TRE))));
		mano.punta(3);
		mano.vedo();
		mano.vedo();
		mano.vedo();

		mano.dichiara(mano.dichiarante(), 20);
		mano.contesta(1);

		assertTrue(mano.chiusa());
		assertEquals(List.of(1), mano.vincitori());
		assertEquals("non-sei-buono", mano.esitoDichiarazione());
	}

	@Test
	void chiHaAccettatoNonPuoPiuContestare() {
		Mano mano = mano();
		mano.punta(3);
		mano.vedo();
		mano.vedo();
		mano.vedo();

		mano.dichiara(mano.dichiarante(), 20);
		// B ha venticinque punti: senza la regola potrebbe contestare
		mano.accetta(1);
		IllegalStateException errore = assertThrows(IllegalStateException.class, () -> mano.contesta(1));

		assertTrue(errore.getMessage().contains("accettato"), errore.getMessage());
		assertFalse(mano.chiusa());
	}

	@Test
	void chiHaDichiaratoNonContestaSeStesso() {
		Mano mano = mano();
		mano.punta(3);
		mano.vedo();
		mano.vedo();
		mano.vedo();

		mano.dichiara(mano.dichiarante(), 20);
		int dichiarante = mano.dichiarante();

		assertThrows(IllegalStateException.class, () -> mano.contesta(dichiarante));
		assertFalse(mano.chiusa());
	}

	@Test
	void nonSiPuoContestareConMenoDelPuntoDichiarato() {
		Mano mano = mano();
		mano.punta(3);
		mano.vedo();
		mano.vedo();
		mano.vedo();

		mano.dichiara(mano.dichiarante(), 20);
		IllegalStateException errore = assertThrows(IllegalStateException.class, () -> mano.contesta(3));

		assertTrue(errore.getMessage().contains("contestare"), errore.getMessage());
		assertFalse(mano.chiusa());
	}

	@Test
	void nonSiPuoDichiarareFuoriDallaDichiarazione() {
		Mano mano = mano();
		mano.punta(2);

		assertThrows(IllegalStateException.class, () -> mano.dichiara(0, 20));
	}

	@Test
	void siPuoPuntareADebitoEIlCambioCopreLaMancanza() {
		Mano mano = mano();
		a.scalaFiches(6);
		assertEquals(4, a.fiches());

		mano.punta(10);
		assertEquals(-6, a.fiches());
		assertEquals(6, a.debito());

		assertTrue(a.cambio());
		assertEquals(4, a.fiches());
		assertFalse(a.cambio());
	}

}
