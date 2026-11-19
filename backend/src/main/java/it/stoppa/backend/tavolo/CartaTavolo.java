package it.stoppa.backend.tavolo;

import java.util.Locale;

import it.stoppa.backend.game.Carta;

/**
 * Una carta come la vede il browser: a che file SVG corrisponde e quanto vale.
 */
public record CartaTavolo(String seme, String valore, String file, int punti) {

	public static CartaTavolo da(Carta carta) {
		return new CartaTavolo(carta.seme().name(), carta.valore().name(), nomeFile(carta), carta.punti());
	}

	private static String nomeFile(Carta carta) {
		String seme = carta.seme().name().toLowerCase(Locale.ROOT);
		String valore = switch (carta.valore()) {
			case ASSO -> "asso";
			case FANTE -> "fante";
			case CAVALLO -> "cavallo";
			case RE -> "re";
			default -> String.format("%02d", carta.valore().ordine());
		};
		return seme + "-" + valore;
	}

}
