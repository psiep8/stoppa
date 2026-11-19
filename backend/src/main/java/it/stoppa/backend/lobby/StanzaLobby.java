package it.stoppa.backend.lobby;

import java.util.List;

public record StanzaLobby(String codice, List<PostoLobby> posti, int postiTotali, int minimoGiocatori) {

	public int occupati() {
		return posti.size();
	}

}
