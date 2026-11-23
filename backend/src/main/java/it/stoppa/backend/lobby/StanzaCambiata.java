package it.stoppa.backend.lobby;

/**
 * Pubblicato dopo ogni cambiamento della stanza: un ingresso, un bot aggiunto,
 * un posto tolto, la partita avviata. Il socket della stanza lo ascolta e manda
 * la vista aggiornata a chi e' collegato, cosi' la lobby non deve interrogare
 * il server per sapere cosa succede.
 */
public record StanzaCambiata(String codice) {
}
