package it.stoppa.backend.lobby;

/** La risposta a chi crea o entra in una stanza: la propria sessione e la stanza. */
public record IngressoLobby(String sessione, StanzaLobby stanza) {
}
