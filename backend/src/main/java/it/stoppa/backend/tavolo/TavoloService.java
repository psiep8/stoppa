package it.stoppa.backend.tavolo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Service;

import it.stoppa.backend.game.Giocatore;
import it.stoppa.backend.game.Mano;
import it.stoppa.backend.game.Partita;

/**
 * Il primo tavolo giocabile: una partita sola in memoria, tu contro tre bot.
 * Ogni tua mossa fa giocare i bot fino al turno successivo, cosi' il client
 * riceve sempre uno stato pronto da mostrare.
 */
@Service
public class TavoloService {

	public static final int INDICE_UMANO = 0;

	public static final String NOME_UMANO = "Tu";

	private static final int MAX_EVENTI = 30;

	private final Random random = new Random();

	private final Deque<String> eventi = new ArrayDeque<>();

	private Partita partita;

	public synchronized StatoTavolo stato() {
		if (partita == null) {
			return nuovaPartita();
		}
		return statoCorrente();
	}

	public synchronized StatoTavolo nuovaPartita() {
		List<String> nomi = new ArrayList<>();
		nomi.add(NOME_UMANO);
		nomi.add("Bot 1");
		nomi.add("Bot 2");
		nomi.add("Bot 3");
		partita = new Partita(nomi, random);
		eventi.clear();
		aggiungiEvento("Carte distribuite · " + partita.maniTotali() + " mani");
		giocaBot();
		if (partita.mano().chiusa()) {
			aggiungiEvento(esitoMano());
		}
		return statoCorrente();
	}

	public synchronized StatoTavolo passa() {
		verificaTurno();
		partita.passa();
		aggiungiEvento("Passi");
		return dopoLaMossa();
	}

	public synchronized StatoTavolo vedo() {
		verificaTurno();
		partita.vedo();
		aggiungiEvento("Vedi");
		return dopoLaMossa();
	}

	public synchronized StatoTavolo punta(int gettoni) {
		verificaTurno();
		partita.punta(gettoni);
		aggiungiEvento("Punti " + gettoni);
		return dopoLaMossa();
	}

	public synchronized StatoTavolo prossimaMano() {
		if (partita == null) {
			throw new IllegalStateException("nessuna partita in corso");
		}
		if (partita.finita()) {
			return statoCorrente();
		}
		if (!partita.mano().chiusa()) {
			throw new IllegalStateException("la mano " + partita.numeroMano() + " e' ancora aperta");
		}
		partita.prossimaMano();
		if (partita.finita()) {
			Giocatore primo = partita.giocatori().get(partita.classifica().getFirst());
			aggiungiEvento("Partita finita · vince " + primo.nome() + " con " + primo.punteggio() + " punti");
			return statoCorrente();
		}
		aggiungiEvento("Mano " + partita.numeroMano() + " · cartaro " + partita.giocatori().get(partita.cartaro()).nome());
		giocaBot();
		if (partita.mano().chiusa()) {
			aggiungiEvento(esitoMano());
		}
		return statoCorrente();
	}

	private StatoTavolo dopoLaMossa() {
		if (!partita.mano().chiusa()) {
			giocaBot();
		}
		if (partita.mano().chiusa()) {
			aggiungiEvento(esitoMano());
		}
		return statoCorrente();
	}

	private void verificaTurno() {
		if (partita == null) {
			throw new IllegalStateException("nessuna partita in corso");
		}
		if (partita.finita()) {
			throw new IllegalStateException("la partita e' finita: comincia una nuova partita");
		}
		if (partita.mano().chiusa()) {
			throw new IllegalStateException("la mano e' chiusa: passa alla prossima");
		}
		if (partita.mano().giocatoreDiTurno() != INDICE_UMANO) {
			throw new IllegalStateException("non e' il tuo turno");
		}
	}

	private void giocaBot() {
		int guardia = 0;
		while (!partita.finita() && !partita.mano().chiusa()
				&& partita.mano().giocatoreDiTurno() != INDICE_UMANO && guardia++ < 200) {
			mossaBot(partita.mano().giocatoreDiTurno());
		}
	}

	private void mossaBot(int indice) {
		Mano mano = partita.mano();
		String nome = partita.giocatori().get(indice).nome();
		int massima = mano.puntataMassima();
		int gia = mano.puntate().getOrDefault(indice, 0);

		if (massima == 0) {
			if (random.nextInt(100) < 20) {
				partita.passa();
				aggiungiEvento(nome + " passa");
				return;
			}
			int gettoni = 1 + random.nextInt(3);
			partita.punta(gettoni);
			aggiungiEvento(nome + " punta " + gettoni);
			return;
		}

		int scelta = random.nextInt(100);
		if (scelta < 20) {
			partita.passa();
			aggiungiEvento(nome + " passa");
			return;
		}
		if (scelta < 85 || massima >= Mano.TETTO_PUNTATA || gia >= massima) {
			partita.vedo();
			aggiungiEvento(nome + " vede");
			return;
		}
		int gettoni = Math.min(massima + 1 + random.nextInt(2), Mano.TETTO_PUNTATA);
		partita.punta(gettoni);
		aggiungiEvento(nome + " rilancia a " + gettoni);
	}

	private String esitoMano() {
		Mano mano = partita.mano();
		if (mano.senzaPuntate()) {
			return "Mano " + mano.numero() + ": nessuno ha puntato, 1 gettone al cartaro";
		}
		String vincitore = partita.giocatori().get(mano.vincitori().getFirst()).nome();
		return "Mano " + mano.numero() + ": vince " + vincitore;
	}

	private StatoTavolo statoCorrente() {
		Mano mano = partita.mano();
		List<GiocatoreTavolo> giocatori = new ArrayList<>();
		for (int i = 0; i < partita.giocatori().size(); i++) {
			Giocatore giocatore = partita.giocatori().get(i);
			boolean sonoIo = i == INDICE_UMANO;
			giocatori.add(new GiocatoreTavolo(i, giocatore.nome(), giocatore.fiches(), giocatore.debito(),
					mano.puntate().getOrDefault(i, 0), mano.attivi().contains(i),
					!mano.chiusa() && mano.giocatoreDiTurno() == i, !sonoIo, mano.carteDi(i).size(),
					sonoIo ? carte(mano.carteDi(i)) : List.of()));
		}

		ManoTavolo manoTavolo = new ManoTavolo(mano.numero(), partita.maniTotali(),
				partita.giocatori().get(mano.cartaro()).nome(), partita.piatto(), mano.puntataMassima(), mano.chiusa(),
				mano.senzaPuntate(),
				mano.vincitori().stream().map(i -> partita.giocatori().get(i).nome()).toList(),
				carte(mano.carteVincenti()));

		boolean toccaATe = !partita.finita() && !mano.chiusa() && mano.giocatoreDiTurno() == INDICE_UMANO;

		return new StatoTavolo(INDICE_UMANO, NOME_UMANO, giocatori, manoTavolo, toccaATe, partita.finita(),
				List.copyOf(eventi), partita.finita() ? classifica() : List.of(), mano.puntataMassima() + 1,
				Mano.TETTO_PUNTATA);
	}

	private List<ClassificaTavolo> classifica() {
		List<ClassificaTavolo> righe = new ArrayList<>();
		int posizione = 1;
		for (int indice : partita.classifica()) {
			Giocatore giocatore = partita.giocatori().get(indice);
			righe.add(new ClassificaTavolo(posizione++, giocatore.nome(), giocatore.punteggio(), giocatore.fiches()));
		}
		return righe;
	}

	private static List<CartaTavolo> carte(List<it.stoppa.backend.game.Carta> carte) {
		return carte.stream().map(CartaTavolo::da).toList();
	}

	private void aggiungiEvento(String testo) {
		eventi.addFirst(testo);
		while (eventi.size() > MAX_EVENTI) {
			eventi.removeLast();
		}
	}

}
