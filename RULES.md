# Regole della stoppa

Regole del tavolo come concordate, e come sono implementate in
`backend/src/main/java/it/stoppa/backend/game/`.

## Tavolo e mazzo

- Da **4 a 8 giocatori**.
- Mazzo di **40 carte napoletane** (denari, coppe, spade, bastoni: asso, 2-7, fante,
  cavallo, re).
- Le carte si distribuiscono tutte, in modo che ognuno abbia lo stesso numero di
  carte. Se 40 non si divide per il numero di giocatori, si scartano tante carte
  quante il resto, **scegliendo le figure** (le meno preziose).
- Le carte di ogni giocatore si dividono in mani da **massimo 3 carte**:

  | Giocatori | Carte a testa | Mani | Scarti |
  | --------- | ------------- | ---- | ------ |
  | 4         | 10            | 3+3+3+1 | 0   |
  | 5         | 8             | 3+3+2   | 0   |
  | 6         | 6             | 3+3     | 4   |
  | 7         | 5             | 3+2     | 5   |
  | 8         | 5             | 3+2     | 0   |

- Nessuno puo' avere piu' di tre carte dello stesso seme.

## Valori delle carte (primiera)

| Carta | Punti | Carta | Punti |
| ----- | ----- | ----- | ----- |
| Figura (fante, cavallo, re) | 10 | Asso | 16 |
| 2 | 12 | 6 | 18 |
| 3 | 13 | 7 | 21 |
| 4 | 14 | | |
| 5 | 15 | | |

## Punto

Il punto di una mano e' il migliore tra:

- la somma delle carte **dello stesso seme** (al massimo tre carte per seme), e
- la **singola carta di valore piu' alto** tra semi diversi.

Il massimo e' **55**: 7 + 6 + asso dello stesso seme.

Alla fine della partita le carte di tutte le mani di ogni giocatore si mettono
insieme e si rifa' il punto con lo stesso criterio: vince chi totalizza piu' punti.

## Puntate

- Ogni giocatore parte con **10 gettoni**.
- All'inizio della partita ognuno mette **1 gettone nel piatto del tavolo** (una
  sola volta, non a ogni mano): restano quindi **9 gettoni spendibili** a testa,
  piu' quelli che arrivano dalle mani vinte.
- Ogni mano ha il suo giro di puntate. Apre il **cartaro**, poi si gira in senso
  orario; il cartaro ruota a ogni mano.
- Nel proprio turno si puo':
  - **passare**: si esce dalla mano perdendo quello che si e' gia' messo;
  - **vedere**: si pareggia la puntata piu' alta;
  - **puntare**: si alza la posta, da 1 a **10 gettoni**.
- Il giro si chiude quando tutti quelli rimasti in gioco hanno messo lo stesso
  numero di gettoni, oppure quando ne resta uno solo. Chi pareggia blocca i
  rilanci: si girano le carte e si vede chi ha vinto.
- **Chi vince** prende le puntate di tutti e **1 gettone dal piatto**.
- Se in una mano **non punta nessuno**, il gettone del tavolo va al **cartaro**.
- Si puo' puntare **a debito**: i gettoni mancanti vanno al vincitore. Quando un
  giocatore resta senza gettoni scatta il **cambio automatico da 10**, che copre
  quello che deve.

## Cosa si vede

- Le carte di chi non vince **non si mostrano mai**, neanche a fine partita: si
  vedono solo le carte vincenti di ogni mano.
- I gettoni di tutti sono visibili. Il **pulsante a occhio** per decidere se
  mostrare i propri **non e' ancora implementato**: oggi i gettoni sono sempre
  visibili.
- Chi crea la lobby ha un **riepilogo sempre aggiornato** di gettoni, debiti,
  piatto e punto di ogni giocatore. L'API c'e' (`GET /api/tavolo/{codice}/riepilogo`,
  riservata a chi crea la stanza), **l'interfaccia no**: il riepilogo non e' ancora
  mostrato nel tavolo.

## Punto finale

Le carte restanti nel piatto vanno a chi ha il punto piu' alto. A parita' di
punto vince chi ha puntato per primo nella mano; a parita' nella classifica
finale vince chi sta prima nell'ordine del tavolo.
