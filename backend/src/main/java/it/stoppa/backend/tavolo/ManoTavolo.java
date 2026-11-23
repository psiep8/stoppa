package it.stoppa.backend.tavolo;

import java.util.List;

public record ManoTavolo(int numero, int maniTotali, String cartaro, int piatto, int puntataMassima, boolean chiusa,
		boolean senzaPuntate, List<String> vincitori, List<CartaTavolo> carteVincenti, String dichiarante,
		int puntiVincenti, boolean inDichiarazione, int dichiarazione, String esitoDichiarazione) {
}
