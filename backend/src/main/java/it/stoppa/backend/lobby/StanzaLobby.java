package it.stoppa.backend.lobby;

import java.util.List;

/**
 * La stanza come la vede un client: i posti occupati, quanti sono in tutto,
 * se e' chiusa agli esterni e se serve il PIN (la seconda chiave).
 *
 * Il PIN non esce mai dal server: qui si dice solo se c'e'. {@code creatore}
 * dice se chi guarda e' quello che ha aperto la stanza.
 */
public record StanzaLobby(String codice, List<PostoLobby> posti, int postiTotali, int minimoGiocatori,
		boolean avviata, boolean chiusa, boolean conPin, boolean creatore, int tuoIndice) {
}
