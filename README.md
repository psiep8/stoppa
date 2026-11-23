# Stoppa

Gioco di carte tradizionale italiano in versione **digitale free-to-play**: lobby online, stanze, fiches virtuali.
Nessuna vincita in denaro, nessun acquisto di fiches con denaro reale, nessun cashout.

## Stack

| Parte | Tecnologia | Versione |
| --- | --- | --- |
| Backend | Java + Spring Boot (Maven) | Java 25 LTS / Spring Boot 4.1.2 |
| Frontend | Angular (standalone) | 22.3.x |
| Trasporto | REST (`/api/**`) + WebSocket (`/ws/lobby`) | — |

## Struttura

```
backend/    API REST di lobby e tavolo, motore di gioco
frontend/   app Angular (desktop + mobile, layout responsive)
```

## Avvio in locale

Backend (porta 8080):

Serve un **JDK 25** (il progetto compila con `java.version=25`).

```bash
cd backend
./mvnw spring-boot:run
```

Frontend (porta 4200, con proxy verso il backend):

```bash
cd frontend
npm install
npm start
```

Poi apri http://localhost:4200, scegli il nome e crea una stanza (oppure entra con il
codice di una stanza già aperta): da lì aggiungi i bot, avvia la partita e condividi il
link d'invito. Per giocare in due, apri una seconda finestra e incolla il codice o il
link della stanza.

## WebSocket

`/ws/lobby` è il socket della stanza, non del tavolo: lobby e partita usano lo stesso
canale. Il client manda `{"tipo":"entra","codice":"ABCD","sessione":"..."}` e riceve
subito `{"tipo":"stanza","stanza":{...}}`; quando la lobby cambia (ingressi, bot, posti,
avvio) il server rimanda la stanza aggiornata, quindi nessuno deve interrogarlo. A
partita avviata arriva anche `{"tipo":"stato","stato":{...}}`, filtrato per la sessione,
e da quel momento ogni mossa (`passa`, `vedo`, `punta` con `gettoni`, `prossima-mano`,
`nuova-partita`) arriva a tutti i presenti. `{"tipo":"stato"}` chiede una
risincronizzazione; rimandare `entra` fa lo stesso. Gli errori tornano solo a chi ha
sbagliato come `{"tipo":"errore","messaggio":"..."}`.

Un errore speciale è quello dello stato salvato non valido: se la sessione o la stanza
non esistono più (per esempio dopo un riavvio del backend) l'errore porta in più
`"motivo":"ricomincia"`, e il client cancella la partita salvata e torna alla home
invece di restare fermo su un errore che non si può risolvere. Gli altri errori non
hanno `motivo`.

## Invito e sicurezza della stanza

Ogni stanza ha un codice di quattro lettere, generato dal server e **mai riusato**
(neanche dopo che la stanza è stata chiusa). In lobby c'è il link d'invito
`http://localhost:4200/?stanza=CODICE`: chi lo apre trova il codice già scritto in home.

Chi crea la stanza può aggiungere una **seconda chiave**: un PIN di quattro cifre,
opzionale. Il PIN viaggia solo nella richiesta d'ingresso, non compare nel link e non
esce mai dal server nella vista della stanza, che dice soltanto se serve (`conPin`).

Sempre chi crea la stanza può **chiuderla agli esterni**: chi è già dentro resta, chi
prova a entrare riceve "lobby chiusa" finché non la riapre. I bot, la rimozione dei
posti e l'avvio restano riservati al creatore.

## Verbale della partita

A partita finita il tavolo offre **Scarica il verbale**: il server prepara un documento
HTML autonomo con le fiches giocate, vinte, i cambi e quelle rimaste a ogni giocatore,
più la classifica finale. Serve la propria sessione: `GET /api/tavolo/{codice}/verbale`
con `X-Sessione`.

## Partita salvata

L'interfaccia tiene la partita in corso in `sessionStorage` sotto `stoppa.partita`, come
`{"codice":"ABCD","sessione":"...","schermata":"lobby"|"tavolo"}`: ricaricando la
pagina si riprende la stanza o il tavolo invece di tornare alla home. `sessionStorage` è
per scheda, quindi due finestre restano due giocatori diversi; il nome resta in
`localStorage` sotto `stoppa.nome`.

La partita salvata si cancella quando si torna alla home, anche quando è il server a
dire `motivo: "ricomincia"`. Se lo storage è vietato dal browser si gioca lo stesso, ma
ricaricare riporta alla home.

## Regole

Le regole del tavolo sono in [RULES.md](RULES.md).

## Stato del progetto

Si gioca una partita completa in locale, da soli contro i bot o in più persone nella
stessa stanza, ognuna con la propria sessione.

- **Motore** (`backend/src/main/java/it/stoppa/backend/game/`): distribuzione completa
  del mazzo con gli scarti delle figure che avanzano (4 giocatori: 3+3+3+1, 5: 3+3+2,
  6: 3+3, 7 e 8: 3+2), punto della primiera fino a 55, giro di puntate con passo, vedo e
  rilancio fino a 10 gettoni, piatto pagato un gettone a testa all'inizio della partita,
  gettone al vincitore di ogni mano (o al cartaro se nessuno punta), puntate a debito e
  cambio automatico da 10 gettoni, classifica finale per punto.
- **Lobby** (`/api/lobby`): stanza con codice di quattro lettere mai riusato, fino a 8
  posti (parte con te e cinque bot), link d'invito, PIN opzionale, chiusura agli esterni,
  ingresso con codice, bot da aggiungere e togliere, avvio. Chi crea la stanza riceve un
  id di sessione opaco e lo rimanda in `X-Sessione`.
- **Tavolo** (`/api/tavolo/{codice}`): una partita per stanza. Stato, passo, vedo,
  punta, prossima mano, nuova partita, riepilogo per il creatore e verbale scaricabile.
  Ogni giocatore riceve solo le proprie carte: degli altri vede solo quante ne hanno.
- **Interfaccia** (`frontend/src/app/`): home con il nome, lobby con i posti, tavolo con
  le carte napoletane e il registro delle mani, responsive su desktop e mobile.

Coperto da test: `cd backend && ./mvnw verify`, `cd frontend && npm run build` e
`cd frontend && npm test -- --watch=false`.

### Cosa manca

- **Persistenza**: stanze, sessioni e tavoli vivono in memoria e si perdono al riavvio.
- **Riepilogo a schermo** per chi crea la lobby (l'API c'è, l'interfaccia no) e pulsante a
  occhio per mostrare i propri gettoni. Vedi `RULES.md`.

## Grafica

Le 40 carte napoletane e il dorso sono in `frontend/src/assets/carte/` come SVG
(licenza CC0, vedi `LICENZA.md` accanto), in versione a colori pieni.

## CI

`.github/workflows/ci.yml` compila e testa backend e frontend a ogni push e pull request.

## Nota legale

Il progetto è un gioco di pura simulazione. La stoppa con puntate in denaro reale è
classificata come gioco d'azzardo ed è vietata nei luoghi pubblici; la raccolta online
di giocate in denaro richiede concessione ADM e non è oggetto di questo repository.
