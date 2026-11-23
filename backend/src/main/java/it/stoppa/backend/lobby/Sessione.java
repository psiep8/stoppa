package it.stoppa.backend.lobby;

/**
 * Un giocatore seduto in una stanza. L'id e' opaco, lo genera il server e il
 * client lo rimanda nell'header {@code X-Sessione}.
 *
 * La sessione non porta il numero del posto: il posto si ricava cercando questo
 * id tra i posti della stanza o del tavolo, cosi' non esiste un indice da tenere
 * allineato (e da sbagliare).
 */
public record Sessione(String id, String nome, String codiceStanza, boolean creatore) {
}
