package it.stoppa.backend.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DistribuzioneTest {

	private static Distribuzione distribuisci(int giocatori) {
		return new Mazziere(new Random(7)).distribuisci(giocatori);
	}

	private static List<Integer> tagli(Distribuzione distribuzione) {
		List<Integer> tagli = new ArrayList<>();
		for (ManoDistribuita mano : distribuzione.mani()) {
			tagli.add(mano.carteATesta());
		}
		return tagli;
	}

	@Test
	void ilMazzoHaQuarantaCarteTutteDiverse() {
		List<Carta> mazzo = Mazziere.mazzoCompleto();
		assertEquals(40, mazzo.size());
		assertEquals(40, new HashSet<>(mazzo).size());
	}

	@Test
	void conQuattroGiocatoriSiGiocaTreTreTreUno() {
		Distribuzione distribuzione = distribuisci(4);
		assertEquals(List.of(3, 3, 3, 1), tagli(distribuzione));
		assertEquals(List.of(), distribuzione.scartate());
	}

	@Test
	void conCinqueGiocatoriSiGiocaTreTreDue() {
		Distribuzione distribuzione = distribuisci(5);
		assertEquals(List.of(3, 3, 2), tagli(distribuzione));
		assertEquals(List.of(), distribuzione.scartate());
	}

	@Test
	void conSeiGiocatoriSiGiocaTreTreESiScartanoQuattroFigure() {
		Distribuzione distribuzione = distribuisci(6);
		assertEquals(List.of(3, 3), tagli(distribuzione));
		assertEquals(4, distribuzione.scartate().size());
		distribuzione.scartate().forEach(carta -> assertTrue(carta.valore().figura(), carta + " non e' una figura"));
	}

	@Test
	void conSetteGiocatoriSiGiocaTreDueESiScartanoCinqueFigure() {
		Distribuzione distribuzione = distribuisci(7);
		assertEquals(List.of(3, 2), tagli(distribuzione));
		assertEquals(5, distribuzione.scartate().size());
		distribuzione.scartate().forEach(carta -> assertTrue(carta.valore().figura(), carta + " non e' una figura"));
	}

	@Test
	void conOttoGiocatoriSiGiocaTreDueSenzaScarti() {
		Distribuzione distribuzione = distribuisci(8);
		assertEquals(List.of(3, 2), tagli(distribuzione));
		assertEquals(List.of(), distribuzione.scartate());
	}

	@ParameterizedTest
	@ValueSource(ints = { 4, 5, 6, 7, 8 })
	void nessunaCartaSiPerdeNeSiRipete(int giocatori) {
		Distribuzione distribuzione = distribuisci(giocatori);
		Set<Carta> viste = new HashSet<>(distribuzione.scartate());
		for (ManoDistribuita mano : distribuzione.mani()) {
			assertEquals(giocatori, mano.giocatori());
			for (int giocatore = 0; giocatore < giocatori; giocatore++) {
				for (Carta carta : mano.di(giocatore)) {
					assertTrue(viste.add(carta), "carta ripetuta: " + carta);
				}
			}
		}
		assertEquals(40, viste.size());
	}

	@Test
	void iGiocatoriFuoriRangeSonoRifiutati() {
		for (int giocatori : new int[] { 2, 3, 9 }) {
			try {
				distribuisci(giocatori);
				throw new AssertionError("accettati " + giocatori + " giocatori");
			}
			catch (IllegalArgumentException atteso) {
				assertTrue(atteso.getMessage().contains("giocatori"));
			}
		}
	}

}
