package it.stoppa.backend.lobby;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/**
 * Le sessioni dei giocatori: id opachi generati dal server e tenuti in memoria,
 * come le stanze.
 */
@Service
public class SessioniService {

	public static final String HEADER = "X-Sessione";

	private final Map<String, Sessione> sessioni = new ConcurrentHashMap<>();

	public Sessione crea(String nome, String codiceStanza, boolean creatore) {
		String id = UUID.randomUUID().toString().replace("-", "");
		Sessione sessione = new Sessione(id, nome, codiceStanza, creatore);
		sessioni.put(id, sessione);
		return sessione;
	}

	public Sessione richiesta(String id) {
		if (id == null || id.isBlank()) {
			throw new RicominciaException("sessione mancante: serve l'header " + HEADER);
		}
		Sessione sessione = sessioni.get(id);
		if (sessione == null) {
			throw new RicominciaException("sessione non valida: ricomincia dalla home");
		}
		return sessione;
	}

	public void rimuovi(String id) {
		sessioni.remove(id);
	}

}
