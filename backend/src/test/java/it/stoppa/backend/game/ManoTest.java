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
	void quandoTuttiPareggianoSiGiranoLeCarte() {
		Mano mano = mano();
		mano.punta(3);
		mano.vedo();
		mano.vedo();
		mano.vedo();

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

		assertTrue(mano.chiusa());
		assertEquals(List.of(0), mano.vincitori());
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
