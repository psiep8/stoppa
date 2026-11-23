package it.stoppa.backend.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class PartitaTest {

	private static final List<String> QUATTRO = List.of("A", "B", "C", "D");

	/** Quattro giocatori da dieci gettoni: quattro finiscono subito nel piatto. */
	private static final int GETTONI_IN_TAVOLO = 40;

	private static Partita partita() {
		return new Partita(QUATTRO, new Random(11));
	}

	private static int gettoniInGioco(Partita partita) {
		return partita.piatto() + partita.giocatori().stream().mapToInt(Giocatore::fiches).sum();
	}

	@Test
	void aInizioPartitaOgnunoPagaUnGettoneNelPiatto() {
		Partita partita = partita();

		assertEquals(4, partita.piatto());
		assertEquals(4, partita.maniTotali());
		assertEquals(1, partita.numeroMano());
		assertEquals(0, partita.cartaro());
		partita.giocatori().forEach(giocatore -> assertEquals(9, giocatore.fiches()));
		assertEquals(GETTONI_IN_TAVOLO, gettoniInGioco(partita));
	}

	@Test
	void servonoDaQuattroAOttogiocatori() {
		assertThrows(IllegalArgumentException.class, () -> new Partita(List.of("A", "B", "C"), new Random(1)));
		assertThrows(IllegalArgumentException.class,
				() -> new Partita(List.of("A", "B", "C", "D", "E", "F", "G", "H", "I"), new Random(1)));
	}

	@Test
	void unaManoSenzaPuntatePagaUnGettoneAlCartaro() {
		Partita partita = partita();
		for (int i = 0; i < 4; i++) {
			partita.passa();
		}

		assertTrue(partita.mano().chiusa());
		assertEquals(3, partita.piatto());
		assertEquals(10, partita.giocatori().get(partita.cartaro()).fiches());
		assertEquals(GETTONI_IN_TAVOLO, gettoniInGioco(partita));
	}

	@Test
	void chiVincePrendeLePuntateEUnGettoneDalPiatto() {
		Partita partita = partita();
		partita.punta(2);
		partita.vedo();
		partita.vedo();
		partita.vedo();

		assertTrue(partita.mano().inDichiarazione());
		partita.dichiara(partita.mano().dichiarante(), 1);
		partita.contesta((partita.mano().dichiarante() + 1) % 4);

		assertTrue(partita.mano().chiusa());
		int vincitore = partita.mano().vincitori().getFirst();
		assertEquals(16, partita.giocatori().get(vincitore).fiches());
		assertEquals(3, partita.piatto());
		assertEquals(GETTONI_IN_TAVOLO, gettoniInGioco(partita));
	}

	@Test
	void laManoSeguenteCambiaCartaro() {
		Partita partita = partita();
		for (int i = 0; i < 4; i++) {
			partita.passa();
		}

		partita.prossimaMano();

		assertEquals(2, partita.numeroMano());
		assertEquals(1, partita.cartaro());
	}

	@Test
	void aFinePartitaIlPiattoAvanzaAlPuntoPiuAlto() {
		Partita partita = partita();
		for (int mano = 0; mano < partita.maniTotali(); mano++) {
			for (int i = 0; i < 4; i++) {
				partita.passa();
			}
			partita.prossimaMano();
		}

		assertTrue(partita.finita());
		assertEquals(0, partita.piatto());
		assertEquals(GETTONI_IN_TAVOLO, gettoniInGioco(partita));
	}

	@Test
	void ilRiepilogoRaccontaIlTavolo() {
		Partita partita = partita();
		partita.punta(3);

		RiepilogoPartita riepilogo = partita.riepilogo();

		assertEquals(4, riepilogo.giocatori().size());
		assertEquals(partita.giocatori().getFirst().nome(), riepilogo.giocatori().getFirst().nome());
		assertEquals(3, riepilogo.giocatori().getFirst().puntata());
		assertEquals(6, riepilogo.giocatori().getFirst().fiches());
		assertEquals(partita.giocatori().get(partita.cartaro()).nome(), riepilogo.cartaro());
	}

}
