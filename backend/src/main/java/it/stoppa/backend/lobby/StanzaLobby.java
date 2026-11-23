package it.stoppa.backend.lobby;

import java.util.List;

/**
 * La stanza come la vede un client: i posti occupati, quanti sono in tutto e
 * quale posto occupa chi chiede (indice -1 se non e' seduto qui).
 */
public record StanzaLobby(String codice, List<PostoLobby> posti, int postiTotali, int minimoGiocatori,
		boolean avviata, int tuoIndice) {
}
