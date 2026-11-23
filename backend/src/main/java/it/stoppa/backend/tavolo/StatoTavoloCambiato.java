package it.stoppa.backend.tavolo;

/**
 * Pubblicato dopo ogni mossa riuscita su un tavolo. Il socket della stanza lo
 * ascolta e manda lo stato aggiornato a tutti i presenti, cosi' una mossa fatta
 * via REST arriva comunque anche a chi e' collegato in WebSocket.
 */
public record StatoTavoloCambiato(String codice) {
}
