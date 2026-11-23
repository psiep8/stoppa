package it.stoppa.backend.tavolo;

import java.util.Locale;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.stoppa.backend.game.RiepilogoPartita;
import it.stoppa.backend.lobby.SessioniService;

@RestController
@RequestMapping("/api/tavolo/{codice}")
public class TavoloController {

	private final TavoloService tavolo;

	public TavoloController(TavoloService tavolo) {
		this.tavolo = tavolo;
	}

	@GetMapping
	public StatoTavolo stato(@PathVariable String codice,
			@RequestHeader(value = SessioniService.HEADER, required = false) String sessione) {
		return tavolo.stato(codice, sessione);
	}

	@GetMapping("/riepilogo")
	public RiepilogoPartita riepilogo(@PathVariable String codice,
			@RequestHeader(value = SessioniService.HEADER, required = false) String sessione) {
		return tavolo.riepilogo(codice, sessione);
	}

	@GetMapping(value = "/verbale", produces = MediaType.TEXT_HTML_VALUE)
	public ResponseEntity<String> verbale(@PathVariable String codice,
			@RequestHeader(value = SessioniService.HEADER, required = false) String sessione) {
		return ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_DISPOSITION,
					"attachment; filename=\"stoppa-" + codice.toUpperCase(Locale.ROOT) + ".html\"")
			.contentType(MediaType.parseMediaType("text/html; charset=UTF-8"))
			.body(tavolo.verbale(codice, sessione));
	}

	@PostMapping("/nuova")
	public StatoTavolo nuova(@PathVariable String codice,
			@RequestHeader(value = SessioniService.HEADER, required = false) String sessione) {
		return tavolo.nuovaPartita(codice, sessione);
	}

	@PostMapping("/passa")
	public StatoTavolo passa(@PathVariable String codice,
			@RequestHeader(value = SessioniService.HEADER, required = false) String sessione) {
		return tavolo.passa(codice, sessione);
	}

	@PostMapping("/vedo")
	public StatoTavolo vedo(@PathVariable String codice,
			@RequestHeader(value = SessioniService.HEADER, required = false) String sessione) {
		return tavolo.vedo(codice, sessione);
	}

	@PostMapping("/punta")
	public StatoTavolo punta(@PathVariable String codice,
			@RequestHeader(value = SessioniService.HEADER, required = false) String sessione,
			@RequestBody PuntataRichiesta richiesta) {
		return tavolo.punta(codice, sessione, richiesta.gettoni());
	}

	@PostMapping("/prossima-mano")
	public StatoTavolo prossimaMano(@PathVariable String codice,
			@RequestHeader(value = SessioniService.HEADER, required = false) String sessione) {
		return tavolo.prossimaMano(codice, sessione);
	}

	public record PuntataRichiesta(int gettoni) {
	}

}
