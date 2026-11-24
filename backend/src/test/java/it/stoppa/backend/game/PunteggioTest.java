package it.stoppa.backend.game;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class PunteggioTest {

	private static Carta carta(Seme seme, Valore valore) {
		return new Carta(seme, valore);
	}

	@Test
	void ilMassimoEIlCinquantacinqueConSetteSeiEAssoDelloStessoSeme() {
		int punti = Punteggio.calcola(List.of(carta(Seme.DENARI, Valore.SETTE), carta(Seme.DENARI, Valore.SEI),
				carta(Seme.DENARI, Valore.ASSO)));

		assertEquals(55, punti);
		assertEquals(Punteggio.MASSIMO, punti);
	}

	@Test
	void unaSolaCartaValeISuoiPunti() {
		assertEquals(21, Punteggio.calcola(List.of(carta(Seme.SPADE, Valore.SETTE))));
	}

	@Test
	void valeLaCartaPiuAltaTraSemiDiversi() {
		assertEquals(21, Punteggio.calcola(
				List.of(carta(Seme.SPADE, Valore.SETTE), carta(Seme.COPPE, Valore.DUE), carta(Seme.BASTONI, Valore.RE))));
	}

	@Test
	void conQuattroCarteDelloStessoSemeContanoSoloLeTreMigliori() {
		int punti = Punteggio.calcola(List.of(carta(Seme.COPPE, Valore.SETTE), carta(Seme.COPPE, Valore.SEI),
				carta(Seme.COPPE, Valore.ASSO), carta(Seme.COPPE, Valore.CINQUE)));

		assertEquals(55, punti);
	}

	@Test
	void conTreCarteNeMostraSoloDueSeIlPuntoLoFannoDue() {
		List<Carta> carte = List.of(carta(Seme.DENARI, Valore.SETTE), carta(Seme.DENARI, Valore.SEI),
				carta(Seme.COPPE, Valore.ASSO));

		assertEquals(39, Punteggio.calcola(carte));
		assertEquals(List.of(carta(Seme.DENARI, Valore.SETTE), carta(Seme.DENARI, Valore.SEI)),
				Punteggio.carteDelPunto(carte));
	}

	@Test
	void seIlPuntoELaCartaPiuAltaMostraSoloQuella() {
		List<Carta> carte = List.of(carta(Seme.DENARI, Valore.DUE), carta(Seme.SPADE, Valore.SETTE),
				carta(Seme.COPPE, Valore.RE));

		assertEquals(List.of(carta(Seme.SPADE, Valore.SETTE)), Punteggio.carteDelPunto(carte));
	}

	@Test
	void seIlPuntoLoFannoTreCarteLeMostraTutte() {
		List<Carta> carte = List.of(carta(Seme.BASTONI, Valore.SETTE), carta(Seme.BASTONI, Valore.SEI),
				carta(Seme.BASTONI, Valore.ASSO), carta(Seme.COPPE, Valore.RE));

		assertEquals(3, Punteggio.carteDelPunto(carte).size());
		assertEquals(55, Punteggio.calcola(Punteggio.carteDelPunto(carte)));
	}

	@Test
	void senzaCarteIlPuntoEZero() {
		assertEquals(0, Punteggio.calcola(List.of()));
	}

}
