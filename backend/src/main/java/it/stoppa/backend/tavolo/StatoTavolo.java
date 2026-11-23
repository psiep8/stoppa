package it.stoppa.backend.tavolo;

import java.util.List;

public record StatoTavolo(int tuoIndice, String nomeTuo, List<GiocatoreTavolo> giocatori, ManoTavolo mano,
		boolean toccaATe, boolean finita, List<String> eventi, int puntataMinima, int puntataMassima,
		boolean puoiDichiarare, boolean puoiRispondere, boolean puoiContestare, int tuoiPunti) {
}