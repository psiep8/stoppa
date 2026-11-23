package it.stoppa.backend.tavolo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import it.stoppa.backend.game.Giocatore;
import it.stoppa.backend.game.Mano;
import it.stoppa.backend.game.Partita;
import it.stoppa.backend.game.Punteggio;
import it.stoppa.backend.game.RiepilogoPartita;
import it.stoppa.backend.lobby.Sessione;
import it.stoppa.backend.lobby.SessioniService;

/**
 * I tavoli delle stanze: una partita per codice stanza, in memoria.
 *
 * Il posto non arriva mai dal client: si cerca l'id della sessione tra i posti
 * del tavolo, quindi le carte si mostrano solo a chi quel posto lo occupa
 * davvero. Gli altri vedono solo quante carte ha ognuno.
 *
 * Dopo ogni mossa i bot giocano fino al turno di un giocatore umano, cosi' chi
 * chiama riceve sempre uno stato pronto da mostrare.
 */
@Service
public class TavoloService {

	private final Map<String, PartitaTavolo> tavoli = new ConcurrentHashMap<>();

	private final SessioniService sessioni;

	private final ApplicationEventPublisher eventi;

	public TavoloService(SessioniService sessioni, ApplicationEventPublisher eventi) {
		this.sessioni = sessioni;
		this.eventi = eventi;
	}

	public synchronized void crea(String codice, List<PostoTavolo> posti) {
		String stanza = chiave(codice);
		if (tavoli.containsKey(stanza)) {
			throw new IllegalStateException("la stanza " + stanza + " ha gia' una partita");
		}
		PartitaTavolo tavolo = new PartitaTavolo(stanza, posti);
		tavoli.put(stanza, tavolo);
		aggiungiEvento(tavolo, "Carte distribuite · " + tavolo.partita.maniTotali() + " mani");
		giocaBot(tavolo);
		if (tavolo.partita.mano().chiusa()) {
			aggiungiEvento(tavolo, esitoMano(tavolo));
		}
	}

	public synchronized StatoTavolo stato(String codice, String sessioneId) {
		Accesso accesso = accedi(codice, sessioneId);
		return statoPer(accesso);
	}

	/**
	 * Fotografia per chi crea la lobby: gettoni, debiti, piatto e punto di ogni
	 * giocatore.
	 */
	public synchronized RiepilogoPartita riepilogo(String codice, String sessioneId) {
		Accesso accesso = accedi(codice, sessioneId);
		if (!accesso.sessione().creatore()) {
			throw new IllegalStateException("il riepilogo e' solo di chi crea la lobby");
		}
		return accesso.tavolo.partita.riepilogo();
	}

	/**
	 * Il verbale della partita: un documento HTML autonomo, da scaricare o
	 * stampare, con le fiches giocate e rimaste a ogni giocatore.
	 */
	public synchronized String verbale(String codice, String sessioneId) {
		Accesso accesso = accedi(codice, sessioneId);
		RiepilogoPartita riepilogo = accesso.tavolo.partita.riepilogo();
		StringBuilder html = new StringBuilder(2048);
		html.append("""
				<!doctype html>
				<html lang="it">
				<head>
				<meta charset="utf-8">
				<meta name="viewport" content="width=device-width, initial-scale=1">
				<title>Stoppa - stanza """);
		html.append(escape(accesso.tavolo.codice)).append("""
				</title>
				<style>
				* { box-sizing: border-box; }
				body { font-family: Georgia, 'Times New Roman', serif; margin: 2.5rem auto; max-width: 46rem; padding: 0 1.2rem; color: #1c2a22; background: #fbf8f0; }
				h1 { margin: 0 0 .2rem; letter-spacing: .16em; text-transform: uppercase; font-size: 1.5rem; }
				h2 { margin-top: 2rem; font-size: 1.05rem; letter-spacing: .08em; text-transform: uppercase; }
				.nota { color: #5b6a60; font-size: .88rem; margin: .25rem 0; }
				table { border-collapse: collapse; width: 100%; margin: 1.4rem 0; }
				th, td { border-bottom: 1px solid #cfc8b8; padding: .5rem .55rem; text-align: right; font-variant-numeric: tabular-nums; }
				th:first-child, td:first-child { text-align: left; }
				th { font-size: .68rem; letter-spacing: .12em; text-transform: uppercase; color: #5b6a60; font-weight: 600; }
				ol { line-height: 1.8; padding-left: 1.4rem; }
				footer { margin-top: 2rem; }
				</style>
				</head>
				<body>
				<h1>Stoppa</h1>
				""");
		html.append("<p class=\"nota\">Stanza <strong>").append(escape(accesso.tavolo.codice))
				.append("</strong> - ").append(riepilogo.maniTotali()).append(" mani - ")
				.append(riepilogo.finita() ? "partita conclusa" : "partita in corso").append("</p>");
		html.append("<p class=\"nota\">Ognuno parte con ").append(Giocatore.FICHES_INIZIALI)
				.append(" gettoni; il gettone del piatto e' compreso tra le fiches giocate e ogni cambio automatico ne aggiunge altri ")
				.append(Giocatore.FICHES_INIZIALI).append(".</p>");
		html.append("""
				<table>
				<thead><tr><th>Giocatore</th><th>Giocate</th><th>Vinte</th><th>Cambi</th><th>Rimaste</th><th>Debito</th><th>Punto</th></tr></thead>
				<tbody>
				""");
		for (RiepilogoPartita.RigaGiocatore riga : riepilogo.giocatori()) {
			html.append("<tr><td>").append(escape(riga.nome())).append("</td><td>").append(riga.giocati())
					.append("</td><td>").append(riga.incassati()).append("</td><td>").append(riga.cambi())
					.append("</td><td>").append(riga.fiches()).append("</td><td>").append(riga.debito())
					.append("</td><td>").append(riga.punteggio()).append("</td></tr>");
		}
		html.append("</tbody></table>");
		if (riepilogo.finita() && !riepilogo.classifica().isEmpty()) {
			html.append("<h2>Classifica</h2><ol>");
			for (int indice : riepilogo.classifica()) {
				RiepilogoPartita.RigaGiocatore riga = riepilogo.giocatori().get(indice);
				html.append("<li><strong>").append(escape(riga.nome())).append("</strong> - ")
						.append(riga.punteggio()).append(" punti - ").append(riga.fiches()).append(" fiches</li>");
			}
			html.append("</ol>");
		}
		html.append("<footer><p class=\"nota\">Piatto rimasto: ").append(riepilogo.piatto())
				.append(" - fiches virtuali, nessun denaro reale - ").append(LocalDate.now()).append("</p></footer>");
		html.append("</body></html>");
		return html.toString();
	}

	public synchronized StatoTavolo passa(String codice, String sessioneId) {
		Accesso accesso = accessoDiTurno(codice, sessioneId);
		accesso.tavolo.partita.passa();
		aggiungiEvento(accesso.tavolo, "Passi");
		return dopoLaMossa(accesso);
	}

	public synchronized StatoTavolo vedo(String codice, String sessioneId) {
		Accesso accesso = accessoDiTurno(codice, sessioneId);
		accesso.tavolo.partita.vedo();
		aggiungiEvento(accesso.tavolo, "Vedi");
		return dopoLaMossa(accesso);
	}

	public synchronized StatoTavolo punta(String codice, String sessioneId, int gettoni) {
		Accesso accesso = accessoDiTurno(codice, sessioneId);
		accesso.tavolo.partita.punta(gettoni);
		aggiungiEvento(accesso.tavolo, "Punti " + gettoni);
		return dopoLaMossa(accesso);
	}

	/**
	 * Mostrare o coprire le proprie fiches: e' una scelta personale, gli altri
	 * vedono una pila coperta e nessun numero.
	 */
	public synchronized StatoTavolo mostraFiches(String codice, String sessioneId, boolean mostra) {
		Accesso accesso = accedi(codice, sessioneId);
		if (mostra) {
			accesso.tavolo.fichesInVista.add(sessioneId);
		} else {
			accesso.tavolo.fichesInVista.remove(sessioneId);
		}
		aggiungiEvento(accesso.tavolo, (mostra ? "Mostri" : "Copri") + " le fiches");
		return annuncia(accesso);
	}

	/** Chi apre la mano dichiara il suo punteggio: vero o bluff che sia. */
	public synchronized StatoTavolo dichiara(String codice, String sessioneId, int punti) {
		Accesso accesso = accessoInDichiarazione(codice, sessioneId);
		accesso.tavolo.partita.dichiara(accesso.indice(), punti);
		aggiungiEvento(accesso.tavolo, "Dichiari " + punti);
		return dopoLaMossa(accesso);
	}

	/** Contesta la dichiarazione: il server gira le carte e dice chi ha ragione. */
	public synchronized StatoTavolo contesta(String codice, String sessioneId) {
		Accesso accesso = accessoInDichiarazione(codice, sessioneId);
		accesso.tavolo.partita.contesta(accesso.indice());
		aggiungiEvento(accesso.tavolo, "Contesti");
		return dopoLaMossa(accesso);
	}

	/** Accetta la dichiarazione senza contestarla. */
	public synchronized StatoTavolo accetta(String codice, String sessioneId) {
		Accesso accesso = accessoInDichiarazione(codice, sessioneId);
		accesso.tavolo.partita.accetta(accesso.indice());
		aggiungiEvento(accesso.tavolo, "Accetti");
		return dopoLaMossa(accesso);
	}

	public synchronized StatoTavolo prossimaMano(String codice, String sessioneId) {
		Accesso accesso = accedi(codice, sessioneId);
		Partita partita = accesso.tavolo.partita;
		if (partita.finita()) {
			return statoPer(accesso);
		}
		if (!partita.mano().chiusa()) {
			throw new IllegalStateException("la mano " + partita.numeroMano() + " e' ancora aperta");
		}
		partita.prossimaMano();
		if (partita.finita()) {
			Giocatore primo = partita.giocatori().get(partita.classifica().getFirst());
			aggiungiEvento(accesso.tavolo,
					"Partita finita · vince " + primo.nome() + " con " + primo.punteggio() + " punti");
			return annuncia(accesso);
		}
		aggiungiEvento(accesso.tavolo,
				"Mano " + partita.numeroMano() + " · cartaro " + partita.giocatori().get(partita.cartaro()).nome());
		giocaBot(accesso.tavolo);
		if (partita.mano().chiusa()) {
			aggiungiEvento(accesso.tavolo, esitoMano(accesso.tavolo));
		}
		return annuncia(accesso);
	}

	public synchronized StatoTavolo nuovaPartita(String codice, String sessioneId) {
		Accesso accesso = accedi(codice, sessioneId);
		if (!accesso.sessione().creatore()) {
			throw new IllegalStateException("solo chi crea la lobby puo' ricominciare");
		}
		PartitaTavolo tavolo = accesso.tavolo;
		tavolo.partita = tavolo.nuovaPartita();
		tavolo.eventi.clear();
		tavolo.fichesInVista.clear();
		aggiungiEvento(tavolo, "Carte distribuite · " + tavolo.partita.maniTotali() + " mani");
		giocaBot(tavolo);
		if (tavolo.partita.mano().chiusa()) {
			aggiungiEvento(tavolo, esitoMano(tavolo));
		}
		return annuncia(accesso);
	}

	/**
	 * Lo stato per chi ha mosso, annunciato alla stanza: REST e socket passano
	 * entrambi di qui, quindi ogni mossa riuscita arriva a tutti i presenti.
	 */
	private StatoTavolo annuncia(Accesso accesso) {
		eventi.publishEvent(new StatoTavoloCambiato(accesso.tavolo.codice));
		return statoPer(accesso);
	}

	private StatoTavolo dopoLaMossa(Accesso accesso) {
		if (!accesso.tavolo.partita.mano().chiusa()) {
			giocaBot(accesso.tavolo);
		}
		if (accesso.tavolo.partita.mano().chiusa()) {
			aggiungiEvento(accesso.tavolo, esitoMano(accesso.tavolo));
		}
		return annuncia(accesso);
	}

	private Accesso accedi(String codice, String sessioneId) {
		Sessione sessione = sessioni.richiesta(sessioneId);
		String stanza = chiave(codice);
		if (!sessione.codiceStanza().equals(stanza)) {
			throw new IllegalStateException("non sei seduto nella stanza " + stanza);
		}
		PartitaTavolo tavolo = tavoli.get(stanza);
		if (tavolo == null) {
			throw new IllegalStateException("la stanza " + stanza + " non ha una partita avviata");
		}
		return new Accesso(tavolo, sessione, tavolo.indiceDi(sessione.id()));
	}

	private static String chiave(String codice) {
		return codice == null ? "" : codice.toUpperCase(Locale.ROOT);
	}

	private Accesso accessoInDichiarazione(String codice, String sessioneId) {
		Accesso accesso = accedi(codice, sessioneId);
		if (accesso.tavolo.partita.finita()) {
			throw new IllegalStateException("la partita e' finita: comincia una nuova partita");
		}
		if (!accesso.tavolo.partita.mano().inDichiarazione()) {
			throw new IllegalStateException("non c'e' una dichiarazione aperta");
		}
		return accesso;
	}

	private Accesso accessoDiTurno(String codice, String sessioneId) {
		Accesso accesso = accedi(codice, sessioneId);
		if (accesso.tavolo.partita.finita()) {
			throw new IllegalStateException("la partita e' finita: comincia una nuova partita");
		}
		if (accesso.tavolo.partita.mano().chiusa()) {
			throw new IllegalStateException("la mano e' chiusa: passa alla prossima");
		}
		if (accesso.tavolo.partita.mano().giocatoreDiTurno() != accesso.indice()) {
			throw new IllegalStateException("non e' il tuo turno");
		}
		return accesso;
	}

	private void giocaBot(PartitaTavolo tavolo) {
		int guardia = 0;
		while (!tavolo.partita.finita() && !tavolo.partita.mano().chiusa() && guardia++ < 300) {
			Mano mano = tavolo.partita.mano();
			if (mano.inDichiarazione()) {
				if (!dichiarazioneBot(tavolo, mano)) {
					return;
				}
				continue;
			}
			if (!tavolo.posti.get(mano.giocatoreDiTurno()).bot()) {
				return;
			}
			mossaBot(tavolo, mano.giocatoreDiTurno());
		}
	}

	/**
	 * I bot dichiarano e rispondono da soli; se tocca a un umano la partita si
	 * ferma e aspetta la sua mossa.
	 */
	private boolean dichiarazioneBot(PartitaTavolo tavolo, Mano mano) {
		if (mano.dichiarazione() == 0) {
			int chi = mano.dichiarante();
			if (chi < 0 || !tavolo.posti.get(chi).bot()) {
				return false;
			}
			int reali = Punteggio.calcola(mano.carteDi(chi));
			int finti = Math.min(reali + tavolo.random.nextInt(3), Mano.TETTO_DICHIARAZIONE);
			tavolo.partita.dichiara(chi, Math.max(1, finti));
			aggiungiEvento(tavolo, tavolo.partita.giocatori().get(chi).nome() + " dichiara " + finti);
			return true;
		}
		for (int giocatore : mano.attivi()) {
			if (giocatore == mano.dichiarante() || mano.accettato().contains(giocatore)
					|| !tavolo.posti.get(giocatore).bot()) {
				continue;
			}
			String nome = tavolo.partita.giocatori().get(giocatore).nome();
			int reali = Punteggio.calcola(mano.carteDi(giocatore));
			if (reali >= mano.dichiarazione()) {
				tavolo.partita.contesta(giocatore);
				aggiungiEvento(tavolo, nome + " contesta");
			} else {
				tavolo.partita.accetta(giocatore);
				aggiungiEvento(tavolo, nome + " accetta");
			}
			return true;
		}
		return false;
	}

	private void mossaBot(PartitaTavolo tavolo, int indice) {
		Mano mano = tavolo.partita.mano();
		String nome = tavolo.partita.giocatori().get(indice).nome();
		int massima = mano.puntataMassima();
		int gia = mano.puntate().getOrDefault(indice, 0);

		if (massima == 0) {
			if (tavolo.random.nextInt(100) < 20) {
				tavolo.partita.passa();
				aggiungiEvento(tavolo, nome + " passa");
				return;
			}
			int gettoni = 1 + tavolo.random.nextInt(3);
			tavolo.partita.punta(gettoni);
			aggiungiEvento(tavolo, nome + " punta " + gettoni);
			return;
		}

		int scelta = tavolo.random.nextInt(100);
		if (scelta < 20) {
			tavolo.partita.passa();
			aggiungiEvento(tavolo, nome + " passa");
			return;
		}
		if (scelta < 85 || massima >= Mano.TETTO_PUNTATA || gia >= massima) {
			tavolo.partita.vedo();
			aggiungiEvento(tavolo, nome + " vede");
			return;
		}
		int gettoni = Math.min(massima + 1 + tavolo.random.nextInt(2), Mano.TETTO_PUNTATA);
		tavolo.partita.punta(gettoni);
		aggiungiEvento(tavolo, nome + " rilancia a " + gettoni);
	}

	private String esitoMano(PartitaTavolo tavolo) {
		Mano mano = tavolo.partita.mano();
		if (mano.senzaPuntate()) {
			return "Mano " + mano.numero() + ": nessuno ha puntato, 1 gettone al cartaro";
		}
		String vincitore = tavolo.partita.giocatori().get(mano.vincitori().getFirst()).nome();
		return "Mano " + mano.numero() + ": vince " + vincitore;
	}

	private StatoTavolo statoPer(Accesso accesso) {
		PartitaTavolo tavolo = accesso.tavolo;
		int indice = accesso.indice();
		Partita partita = tavolo.partita;
		Mano mano = partita.mano();
		List<GiocatoreTavolo> giocatori = new ArrayList<>();
		for (int i = 0; i < partita.giocatori().size(); i++) {
			Giocatore giocatore = partita.giocatori().get(i);
			boolean sonoIo = i == indice;
			PostoTavolo posto = tavolo.posti.get(i);
			// "fichesInVista" e' la scelta del giocatore, non il fatto che io le veda:
			// le mie le vedo sempre, gli altri le vedono solo se le mostro.
			boolean fichesInVista = posto.bot()
					|| (posto.sessioneId() != null && tavolo.fichesInVista.contains(posto.sessioneId()));
			giocatori.add(new GiocatoreTavolo(i, giocatore.nome(),
					sonoIo || fichesInVista ? giocatore.fiches() : -1, giocatore.debito(),
					mano.puntate().getOrDefault(i, 0), mano.attivi().contains(i),
					!mano.chiusa() && mano.giocatoreDiTurno() == i, posto.bot(), mano.carteDi(i).size(),
					sonoIo ? carte(mano.carteDi(i)) : List.of(), fichesInVista));
		}

		int puntiVincenti = mano.chiusa() && !mano.senzaPuntate()
				? Punteggio.calcola(mano.carteVincenti())
				: 0;
		String dichiarante = mano.dichiarante() >= 0 ? partita.giocatori().get(mano.dichiarante()).nome() : "";
		ManoTavolo manoTavolo = new ManoTavolo(mano.numero(), partita.maniTotali(),
				partita.giocatori().get(mano.cartaro()).nome(), partita.piatto(), mano.puntataMassima(), mano.chiusa(),
				mano.senzaPuntate(),
				mano.vincitori().stream().map(i -> partita.giocatori().get(i).nome()).toList(),
				carte(mano.carteVincenti()), dichiarante, puntiVincenti, mano.inDichiarazione(), mano.dichiarazione(),
				mano.esitoDichiarazione());

		boolean toccaATe = !partita.finita() && !mano.chiusa() && !mano.inDichiarazione()
				&& mano.giocatoreDiTurno() == indice;
		boolean puoiDichiarare = mano.inDichiarazione() && mano.dichiarazione() == 0 && mano.dichiarante() == indice;
		boolean puoiRispondere = mano.inDichiarazione() && mano.dichiarazione() > 0 && mano.dichiarante() != indice
				&& mano.attivi().contains(indice) && !mano.accettato().contains(indice);
		// non si contesta per finta: serve almeno il punto dichiarato in mano
		boolean puoiContestare = puoiRispondere
				&& Punteggio.calcola(mano.carteDi(indice)) >= mano.dichiarazione();

		return new StatoTavolo(indice, tavolo.posti.get(indice).nome(), giocatori, manoTavolo, toccaATe,
				partita.finita(), List.copyOf(tavolo.eventi), partita.finita() ? classifica(tavolo) : List.of(),
				mano.puntataMassima() + 1, Mano.TETTO_PUNTATA, puoiDichiarare, puoiRispondere, puoiContestare);
	}

	private List<ClassificaTavolo> classifica(PartitaTavolo tavolo) {
		List<ClassificaTavolo> righe = new ArrayList<>();
		int posizione = 1;
		for (int indice : tavolo.partita.classifica()) {
			Giocatore giocatore = tavolo.partita.giocatori().get(indice);
			righe.add(new ClassificaTavolo(posizione++, giocatore.nome(), giocatore.punteggio(), giocatore.fiches()));
		}
		return righe;
	}

	private static List<CartaTavolo> carte(List<it.stoppa.backend.game.Carta> carte) {
		return carte.stream().map(CartaTavolo::da).toList();
	}

	private void aggiungiEvento(PartitaTavolo tavolo, String testo) {
		tavolo.eventi.addFirst(testo);
		while (tavolo.eventi.size() > PartitaTavolo.MAX_EVENTI) {
			tavolo.eventi.removeLast();
		}
	}

	private static String escape(String testo) {
		StringBuilder pulito = new StringBuilder(testo.length());
		for (char carattere : testo.toCharArray()) {
			switch (carattere) {
				case '<' -> pulito.append("&lt;");
				case '>' -> pulito.append("&gt;");
				case '&' -> pulito.append("&amp;");
				case '"' -> pulito.append("&quot;");
				default -> pulito.append(carattere);
			}
		}
		return pulito.toString();
	}

	private record Accesso(PartitaTavolo tavolo, Sessione sessione, int indice) {
	}

}
