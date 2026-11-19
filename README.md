# Stoppa

Gioco di carte tradizionale italiano in versione **digitale free-to-play**: lobby online, stanze, fiches virtuali.
Nessuna vincita in denaro, nessun acquisto di fiches con denaro reale, nessun cashout.

## Stack

| Parte | Tecnologia | Versione |
| --- | --- | --- |
| Backend | Java + Spring Boot (Maven) | Java 25 LTS / Spring Boot 4.1.2 |
| Frontend | Angular (standalone, zoneless) | 22.3.x |
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

## Stato del progetto

Scheletro pronto: health endpoint, CORS, endpoint WebSocket della lobby, proxy di sviluppo,
pagina di verifica. La logica di gioco (regole della stoppa, turni, punteggi, puntate) va
ancora implementata.

## Nota legale

Il progetto è un gioco di pura simulazione. La stoppa con puntate in denaro reale è
classificata come gioco d'azzardo ed è vietata nei luoghi pubblici; la raccolta online
di giocate in denaro richiede concessione ADM e non è oggetto di questo repository.
