package it.stoppa.backend.lobby;

/**
 * Lo stato salvato non vale piu': la sessione o la stanza non esistono, per
 * esempio dopo un riavvio del backend. Chi lo riceve deve ricominciare dalla
 * home, non restare su un errore che non si puo' risolvere.
 *
 * Resta un {@link IllegalStateException} perche' le API la trattano come le
 * altre richieste non valide (409); il socket la distingue con
 * {@code "motivo":"ricomincia"}.
 */
public class RicominciaException extends IllegalStateException {

	public RicominciaException(String messaggio) {
		super(messaggio);
	}

}
