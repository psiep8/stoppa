package it.stoppa.backend.tavolo;

/**
 * Un posto al tavolo: il nome del giocatore e la sessione che lo occupa
 * (null se il posto e' di un bot). Le carte si mostrano solo al posto che ha
 * quella sessione, mai a un indice indicato dal client.
 */
public record PostoTavolo(String sessioneId, String nome) {

	public boolean bot() {
		return sessioneId == null;
	}

	public boolean di(String sessione) {
		return sessioneId != null && sessioneId.equals(sessione);
	}

}
