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
backend/    API REST e WebSocket della lobby
frontend/   app Angular (desktop + mobile, layout responsive)
```

## Avvio in locale

Backend (porta 8080):

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

Poi apri http://localhost:4200: la pagina mostra lo stato di backend e WebSocket.

## Regole

Le regole del tavolo sono in [RULES.md](RULES.md).

## Stato del progetto

Motore di gioco pronto e coperto dai test (`backend/src/main/java/it/stoppa/backend/game/`):

- distribuzione completa del mazzo con gli scarti delle figure che avanzano
  (4 giocatori: 3+3+3+1, 5: 3+3+2, 6: 3+3, 7 e 8: 3+2);
- punto della primiera, massimo 55 (7+6+asso dello stesso seme), ricalcolato sulle
  carte di tutte le mani a fine partita;
- giro di puntate con passo, vedo e rilancio fino a 10 gettoni, chiusura quando tutti
  hanno pareggiato o ne resta uno;
- piatto del tavolo versato una volta a testa, gettone al vincitore di ogni mano (o al
  cartaro se nessuno punta), puntate a debito e cambio automatico da 10 gettoni;
- riepilogo per chi crea la lobby.

Manca il collegamento alla lobby: protocollo WebSocket, stanze e interfaccia di gioco.

## Grafica

Le 40 carte napoletane e il dorso sono in `frontend/src/assets/carte/` come SVG
(licenza CC0, vedi `LICENZA.md` accanto), in versione a colori pieni.

## CI

`.github/workflows/ci.yml` compila e testa backend e frontend a ogni push e pull request.

## Nota legale

Il progetto è un gioco di pura simulazione. La stoppa con puntate in denaro reale è
classificata come gioco d'azzardo ed è vietata nei luoghi pubblici; la raccolta online
di giocate in denaro richiede concessione ADM e non è oggetto di questo repository.
