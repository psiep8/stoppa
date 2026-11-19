package it.stoppa.backend.lobby;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.stoppa.backend.tavolo.StatoTavolo;

@RestController
@RequestMapping("/api/lobby")
public class LobbyController {

	private final LobbyService lobby;

	public LobbyController(LobbyService lobby) {
		this.lobby = lobby;
	}

	@PostMapping
	public StanzaLobby crea(@RequestBody(required = false) CreaRichiesta richiesta) {
		return lobby.crea(richiesta == null ? null : richiesta.nome());
	}

	@GetMapping("/{codice}")
	public StanzaLobby stanza(@PathVariable String codice) {
		return lobby.stanza(codice);
	}

	@PostMapping("/{codice}/bot")
	public StanzaLobby aggiungiBot(@PathVariable String codice) {
		return lobby.aggiungiBot(codice);
	}

	@DeleteMapping("/{codice}/posti/{indice}")
	public StanzaLobby togliPosto(@PathVariable String codice, @PathVariable int indice) {
		return lobby.togliPosto(codice, indice);
	}

	@PostMapping("/{codice}/avvia")
	public StatoTavolo avvia(@PathVariable String codice) {
		return lobby.avvia(codice);
	}

	public record CreaRichiesta(String nome) {
	}

}
