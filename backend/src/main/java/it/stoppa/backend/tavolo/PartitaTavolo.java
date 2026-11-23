package it.stoppa.backend.tavolo;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import it.stoppa.backend.game.Partita;

/**
 * Una partita per stanza: le carte in gioco, i posti e gli eventi da mostrare.
 * Ogni tavolo ha il suo mazzo e i suoi dadi, cosi' le stanze non si mescolano.
 */
final class PartitaTavolo {

	static final int MAX_EVENTI = 30;

	final String codice;

	final List<PostoTavolo> posti;

	final Deque<String> eventi = new ArrayDeque<>();

	final Random random = new Random();

	Partita partita;

	PartitaTavolo(String codice, List<PostoTavolo> posti) {
		this.codice = codice;
		this.posti = List.copyOf(posti);
		Set<String> sessioni = new HashSet<>();
		for (PostoTavolo posto : posti) {
			if (posto.sessioneId() != null && !sessioni.add(posto.sessioneId())) {
				throw new IllegalArgumentException("la sessione " + posto.sessioneId() + " occupa due posti");
			}
		}
		this.partita = nuovaPartita();
	}

	Partita nuovaPartita() {
		return new Partita(posti.stream().map(PostoTavolo::nome).toList(), random);
	}

	/** Il posto che appartiene a questa sessione, o un errore: nessun indice dal client. */
	int indiceDi(String sessioneId) {
		for (int i = 0; i < posti.size(); i++) {
			if (posti.get(i).di(sessioneId)) {
				return i;
			}
		}
		throw new IllegalStateException("la tua sessione non ha un posto a questo tavolo");
	}

}
