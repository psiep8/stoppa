package it.stoppa.backend.lobby;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
	public IngressoLobby crea(@RequestBody(required = false) CreaRichiesta richiesta) {
		return lobby.crea(richiesta == null ? null : richiesta.nome(), richiesta == null ? null : richiesta.pin());
	}

	@GetMapping("/{codice}")
	public StanzaLobby stanza(@PathVariable String codice,
			@RequestHeader(value = SessioniService.HEADER, required = false) String sessione) {
		return lobby.stanza(codice, sessione);
	}

	@PostMapping("/{codice}/entra")
	public IngressoLobby entra(@PathVariable String codice, @RequestBody(required = false) EntraRichiesta richiesta) {
		return lobby.entra(codice, richiesta == null ? null : richiesta.nome(),
				richiesta == null ? null : richiesta.pin());
	}

	@PostMapping("/{codice}/chiusa")
	public StanzaLobby chiusa(@PathVariable String codice, @RequestBody ChiusaRichiesta richiesta,
			@RequestHeader(value = SessioniService.HEADER, required = false) String sessione) {
		return lobby.impostaChiusa(codice, richiesta.chiusa(), sessione);
	}

	@PostMapping("/{codice}/bot")
	public StanzaLobby aggiungiBot(@PathVariable String codice,
			@RequestHeader(value = SessioniService.HEADER, required = false) String sessione) {
		return lobby.aggiungiBot(codice, sessione);
	}

	@DeleteMapping("/{codice}/posti/{indice}")
	public StanzaLobby togliPosto(@PathVariable String codice, @PathVariable int indice,
			@RequestHeader(value = SessioniService.HEADER, required = false) String sessione) {
		return lobby.togliPosto(codice, indice, sessione);
	}

	@PostMapping("/{codice}/avvia")
	public StatoTavolo avvia(@PathVariable String codice,
			@RequestHeader(value = SessioniService.HEADER, required = false) String sessione) {
		return lobby.avvia(codice, sessione);
	}

	public record CreaRichiesta(String nome, String pin) {
	}

	public record EntraRichiesta(String nome, String pin) {
	}

	public record ChiusaRichiesta(boolean chiusa) {
	}

}
