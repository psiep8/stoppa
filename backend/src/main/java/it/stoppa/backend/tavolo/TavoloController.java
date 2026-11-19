package it.stoppa.backend.tavolo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tavolo")
public class TavoloController {

	private final TavoloService tavolo;

	public TavoloController(TavoloService tavolo) {
		this.tavolo = tavolo;
	}

	@GetMapping
	public StatoTavolo stato() {
		return tavolo.stato();
	}

	@PostMapping("/nuova")
	public StatoTavolo nuova() {
		return tavolo.nuovaPartita();
	}

	@PostMapping("/passa")
	public StatoTavolo passa() {
		return tavolo.passa();
	}

	@PostMapping("/vedo")
	public StatoTavolo vedo() {
		return tavolo.vedo();
	}

	@PostMapping("/punta")
	public StatoTavolo punta(@RequestBody PuntataRichiesta richiesta) {
		return tavolo.punta(richiesta.gettoni());
	}

	@PostMapping("/prossima-mano")
	public StatoTavolo prossimaMano() {
		return tavolo.prossimaMano();
	}

	public record PuntataRichiesta(int gettoni) {
	}

}
