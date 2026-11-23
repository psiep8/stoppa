#!/usr/bin/env python3
"""Prova end-to-end della stoppa: 5 giocatori (2 umani + 3 bot), 5 mani.

Uno dei due umani e' il creatore della stanza, l'altro entra col codice.
Il copione gioca via REST, salva ogni stato utile e controlla i conti:
carte, punti, fiches, piatto, debiti e cambi. Salva anche il verbale HTML.

Uso: python3 scripts/prova-partita.py [--base http://localhost:8081]
"""

from __future__ import annotations

import argparse
import json
import random
import re
import sys
import urllib.error
import urllib.request
from pathlib import Path

SEMI = ["denari", "coppe", "spade", "bastoni"]
PUNTI = {
    "asso": 16,
    "due": 12,
    "tre": 13,
    "quattro": 14,
    "cinque": 15,
    "sei": 18,
    "sette": 21,
    "fante": 10,
    "cavallo": 10,
    "re": 10,
}


def punti_mano(carte):
    """Stessa regola del backend: miglior seme fino a tre carte, o carta singola."""
    migliore = 0
    for seme in SEMI:
        valori = sorted(
            (PUNTI[c["valore"].lower()] for c in carte if c["seme"].lower() == seme),
            reverse=True,
        )
        migliore = max(migliore, sum(valori[:3]))
    return migliore


class Api:
    def __init__(self, base):
        self.base = base.rstrip("/")

    def chiama(self, metodo, percorso, corpo=None, sessione=None):
        dati = None if corpo is None else json.dumps(corpo).encode()
        richiesta = urllib.request.Request(self.base + percorso, data=dati, method=metodo)
        if corpo is not None:
            richiesta.add_header("Content-Type", "application/json")
        if sessione:
            richiesta.add_header("X-Sessione", sessione)
        try:
            with urllib.request.urlopen(richiesta, timeout=20) as risposta:
                testo = risposta.read().decode()
                try:
                    return risposta.status, (json.loads(testo) if testo else None)
                except json.JSONDecodeError:
                    return risposta.status, testo
        except urllib.error.HTTPError as errore:
            testo = errore.read().decode()
            try:
                corpo_errore = json.loads(testo) if testo else None
            except json.JSONDecodeError:
                corpo_errore = testo
            return errore.code, corpo_errore


def controlla(esiti, nome, condizione, dettaglio=""):
    esiti.append({"nome": nome, "ok": bool(condizione), "dettaglio": dettaglio})
    stato = "OK " if condizione else "KO "
    print(f"{stato}{nome}" + (f" — {dettaglio}" if dettaglio else ""))


def stato_tavolo(api, codice, sessione):
    stato, corpo = api.chiama("GET", f"/api/tavolo/{codice}", sessione=sessione)
    if stato != 200:
        raise RuntimeError(f"stato non letto ({stato}): {corpo}")
    return corpo


def scegli_mossa(rng, stato, profilo):
    mano = stato["mano"]
    massima = mano["puntataMassima"]
    if profilo == "aggressivo":
        if massima == 0:
            return ("punta", 10)
        if massima < 10:
            return ("punta", 10)
        return ("vedo", None)
    if massima == 0:
        if rng.random() < 0.12:
            return ("passa", None)
        return ("punta", 1 + rng.randrange(3))
    if rng.random() < 0.15:
        return ("passa", None)
    if massima < 10 and rng.random() < 0.30:
        return ("punta", massima + 1)
    return ("vedo", None)


def scegli_dichiarazione(rng, stato, profilo):
    verita = stato["tuoiPunti"]
    if stato["mano"]["bluffRimasti"] == 0:
        return verita
    if profilo == "bluff":
        return 55 if verita < 55 else verita
    if rng.random() < 0.55 or verita >= 52:
        return verita
    return min(55, verita + 1 + rng.randrange(8))


def scegli_risposta(rng, stato):
    if stato["puoiContestare"] and rng.random() < 0.65:
        return "contesta"
    return "accetta"


def invia_mossa(api, codice, sessione, mossa, payload):
    if mossa in ("passa", "vedo", "contesta", "accetta", "prossima-mano"):
        return api.chiama("POST", f"/api/tavolo/{codice}/{mossa}", {}, sessione)
    return api.chiama("POST", f"/api/tavolo/{codice}/{mossa}", payload, sessione)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", default="http://localhost:8081")
    parser.add_argument("--seed", type=int, default=20261123)
    parser.add_argument(
        "--profilo",
        choices=("normale", "bluff", "aggressivo"),
        default="normale",
        help="normale: misto; bluff: dichiara sempre il massimo; aggressivo: punta sempre 10",
    )
    parser.add_argument("--out", default="docs/prova-5-giocatori.json")
    args = parser.parse_args()

    api = Api(args.base)
    rng = random.Random(args.seed)
    esiti = []
    azioni = []
    giri = []
    prove = []

    # ---- lobby: Peppa crea, Marta entra, si resta in cinque ----
    stato, ingresso_p = api.chiama("POST", "/api/lobby", {"nome": "Peppa"})
    if stato != 200:
        sys.exit(f"lobby non creata: {stato} {ingresso_p}")
    codice = ingresso_p["stanza"]["codice"]
    sessione_p = ingresso_p["sessione"]
    print(f"stanza {codice} creata da Peppa, posti: {len(ingresso_p['stanza']['posti'])}")

    stato, ingresso_m = api.chiama(
        "POST", f"/api/lobby/{codice}/entra", {"nome": "Marta"}
    )
    if stato != 200:
        sys.exit(f"Marta non entrata: {stato} {ingresso_m}")
    sessione_m = ingresso_m["sessione"]

    # [Peppa, Bot1..Bot5, Marta] -> togli Bot5 e Bot4 -> 5 posti
    for indice in (5, 4):
        stato, corpo = api.chiama(
            "DELETE", f"/api/lobby/{codice}/posti/{indice}", sessione=sessione_p
        )
        if stato != 200:
            sys.exit(f"posto {indice} non tolto: {stato} {corpo}")

    stato, stanza = api.chiama(
        "GET", f"/api/lobby/{codice}", sessione=sessione_p
    )
    nomi = [posto["nome"] for posto in stanza["posti"]]
    controlla(esiti, "lobby da 5 giocatori", len(nomi) == 5, str(nomi))

    stato, avvio = api.chiama(
        "POST", f"/api/lobby/{codice}/avvia", sessione=sessione_p
    )
    if stato != 200:
        sys.exit(f"partita non avviata: {stato} {avvio}")
    print(f"partita avviata: {avvio['mano']['maniTotali']} mani previste")

    umani = [("Peppa", sessione_p), ("Marta", sessione_m)]

    def leggi_entrambi():
        return {
            nome: stato_tavolo(api, codice, sessione)
            for nome, sessione in umani
        }

    def snap(nome, stato):
        giocatori = [
            {
                "indice": g["indice"],
                "nome": g["nome"],
                "fiches": g["fiches"],
                "debito": g["debito"],
                "puntata": g["puntata"],
                "inGioco": g["inGioco"],
                "diTurno": g["diTurno"],
                "bot": g["bot"],
                "numeroCarte": g["numeroCarte"],
                "fichesInVista": g["fichesInVista"],
            }
            for g in stato["giocatori"]
        ]
        carte = [
            c
            for g in stato["giocatori"]
            if g["indice"] == stato["tuoIndice"]
            for c in g["carte"]
        ]
        return {
            "giocatore": nome,
            "tuoIndice": stato["tuoIndice"],
            "toccaATe": stato["toccaATe"],
            "finita": stato["finita"],
            "tuoiPunti": stato["tuoiPunti"],
            "giocatori": giocatori,
            "carte": carte,
            "mano": stato["mano"],
            "eventi": stato["eventi"][:8],
        }

    carte_per_mano = {}
    carte_controllate = set()
    partita_corrente = 1
    totale_mani_giocate = 0
    snapshot_duo = None
    verbale_html = None
    ultima_mano_chiusa = -1

    # ---- prove d'errore a mano aperta, prima di giocare ----
    def prova(nome, metodo, percorso, corpo=None, sessione=sessione_p, atteso=409):
        stato_p, risposta_p = api.chiama(metodo, percorso, corpo, sessione)
        prove.append(
            {
                "nome": nome,
                "metodo": metodo,
                "percorso": percorso,
                "status": stato_p,
                "risposta": risposta_p,
            }
        )
        controlla(
            esiti,
            f"prova: {nome}",
            stato_p == atteso,
            f"status {stato_p}: {risposta_p}",
        )

    prova("codice stanza inesistente", "GET", "/api/lobby/ZZZZ", sessione=None)
    prova(
        "prossima mano con la mano aperta",
        "POST",
        f"/api/tavolo/{codice}/prossima-mano",
    )
    prova(
        "nuova partita da chi non crea la lobby",
        "POST",
        f"/api/tavolo/{codice}/nuova",
        sessione=sessione_m,
    )
    prova(
        "puntata fuori dal tetto",
        "POST",
        f"/api/tavolo/{codice}/punta",
        {"gettoni": 11},
    )
    prova(
        "dichiarazione fuori dal tetto",
        "POST",
        f"/api/tavolo/{codice}/dichiara",
        {"punti": 56},
    )
    prova(
        "risposta senza dichiarazione aperta",
        "POST",
        f"/api/tavolo/{codice}/accetta",
        sessione=sessione_m,
    )

    for passo in range(400):
        stati = leggi_entrambi()
        p = stati["Peppa"]
        m = stati["Marta"]

        # ---- carte e punti di ogni mano vista dai due umani ----
        for nome, s in (("Peppa", p), ("Marta", m)):
            chiave = (partita_corrente, s["mano"]["numero"], nome)
            if chiave in carte_controllate:
                continue
            io = s["giocatori"][s["tuoIndice"]]
            if io["carte"] and not s["mano"]["chiusa"]:
                carte_controllate.add(chiave)
                carte_per_mano[chiave] = [c["file"] for c in io["carte"]]
                controlla(
                    esiti,
                    f"carte in mano a {nome} coerenti (partita {partita_corrente}, mano {s['mano']['numero']})",
                    len(io["carte"]) == io["numeroCarte"],
                    f"{len(io['carte'])} carte, numeroCarte {io['numeroCarte']}",
                )
                controlla(
                    esiti,
                    f"punto di {nome} coerente con le sue carte (partita {partita_corrente}, mano {s['mano']['numero']})",
                    s["tuoiPunti"] == punti_mano(io["carte"]),
                    f"API {s['tuoiPunti']} vs calcolo {punti_mano(io['carte'])}",
                )
                totale_mano = sum(g["numeroCarte"] for g in s["giocatori"])
                controlla(
                    esiti,
                    f"carte della mano coerenti (partita {partita_corrente}, mano {s['mano']['numero']})",
                    totale_mano == 15 if s["mano"]["numero"] < 3 else totale_mano == 10,
                    f"{totale_mano} carte sul tavolo",
                )

        # ---- momento buono per il mockup: puntate aperte, tre carte in mano ----
        if (
            snapshot_duo is None
            and not p["mano"]["chiusa"]
            and not p["mano"]["inDichiarazione"]
            and len(p["giocatori"][p["tuoIndice"]]["carte"]) == 3
            and any(g["puntata"] > 0 for g in p["giocatori"])
        ):
            snapshot_duo = {
                "partita": partita_corrente,
                "mano": p["mano"]["numero"],
                "Peppa": snap("Peppa", p),
                "Marta": snap("Marta", m),
            }

        if p["finita"]:
            if partita_corrente == 1 and verbale_html is None:
                stato_v, testo = api.chiama(
                    "GET", f"/api/tavolo/{codice}/verbale", sessione=sessione_p
                )
                if stato_v == 200:
                    verbale_html = testo
                stato_r, riepilogo_finale = api.chiama(
                    "GET", f"/api/tavolo/{codice}/riepilogo", sessione=sessione_p
                )
                if stato_r == 200:
                    somma = sum(r["fiches"] for r in riepilogo_finale["giocatori"])
                    cambi = sum(r["cambi"] for r in riepilogo_finale["giocatori"])
                    controlla(
                        esiti,
                        "piatto distribuito a fine partita 1",
                        riepilogo_finale["piatto"] == 0,
                        f"piatto {riepilogo_finale['piatto']}",
                    )
                    controlla(
                        esiti,
                        "conti conservati a fine partita 1",
                        somma == 50 + 10 * cambi,
                        f"fiches {somma} = 50 + 10*{cambi}",
                    )
            if totale_mani_giocate >= 5:
                break
            # seconda partita per arrivare a cinque mani
            stato_n, corpo_n = api.chiama(
                "POST", f"/api/tavolo/{codice}/nuova", sessione=sessione_p
            )
            if stato_n != 200:
                sys.exit(f"nuova partita fallita: {stato_n} {corpo_n}")
            partita_corrente = 2
            ultima_mano_chiusa = -1
            print("partita 2 avviata")
            continue

        if p["mano"]["chiusa"]:
            # Solo Peppa avanza: e' il comportamento che ci aspettiamo dal gioco.
            numero = p["mano"]["numero"]
            chiave_chiusa = (partita_corrente, numero)
            if ultima_mano_chiusa != chiave_chiusa:
                ultima_mano_chiusa = chiave_chiusa
                totale_mani_giocate += 1
                riepilogo_stato, riepilogo = api.chiama(
                    "GET", f"/api/tavolo/{codice}/riepilogo", sessione=sessione_p
                )
                if riepilogo_stato == 200:
                    somma = sum(r["fiches"] for r in riepilogo["giocatori"])
                    cambi = sum(r["cambi"] for r in riepilogo["giocatori"])
                    controlla(
                        esiti,
                        f"conti conservati dopo la mano {numero} (partita {partita_corrente})",
                        somma + riepilogo["piatto"] == 50 + 10 * cambi,
                        f"fiches {somma} + piatto {riepilogo['piatto']} = "
                        f"50 + 10*{cambi}",
                    )
                    for riga in riepilogo["giocatori"]:
                        atteso = (
                            10
                            - riga["giocati"]
                            + riga["incassati"]
                            + 10 * riga["cambi"]
                        )
                        controlla(
                            esiti,
                            f"fiches coerenti per {riga['nome']} dopo la mano {numero}",
                            riga["fiches"] == atteso,
                            f"fiches {riga['fiches']} vs 10-{riga['giocati']}"
                            f"+{riga['incassati']}+10*{riga['cambi']} = {atteso}",
                        )
                        controlla(
                            esiti,
                            f"debito coerente per {riga['nome']} dopo la mano {numero}",
                            riga["debito"] == max(0, -riga["fiches"]),
                            f"debito {riga['debito']} fiches {riga['fiches']}",
                        )
                    giri.append(
                        {
                            "partita": partita_corrente,
                            "mano": numero,
                            "stato_mano": p["mano"],
                            "stato_fine": snap("Peppa", p),
                            "stato_fine_marta": snap("Marta", m),
                            "riepilogo": riepilogo,
                            "eventi": p["eventi"][:10],
                        }
                    )
                if totale_mani_giocate >= 5:
                    print("cinque mani giocate: stop")
                    break
                stato_avanti, dopo = api.chiama(
                    "POST", f"/api/tavolo/{codice}/prossima-mano", {}, sessione_p
                )
                if stato_avanti != 200:
                    controlla(
                        esiti,
                        f"prossima mano dopo la mano {numero}",
                        False,
                        f"{stato_avanti} {dopo}",
                    )
                continue
            # doppio giro chiuso: non deve succedere con un solo driver
            controlla(esiti, "mano chiusa avanzata una volta sola", False, "loop")
            break

        # ---- dichiarazione ----
        agito = False
        for nome, sessione in umani:
            s = stati[nome]
            if not s["mano"]["inDichiarazione"]:
                continue
            if s["puoiDichiarare"]:
                punti = scegli_dichiarazione(rng, s, args.profilo)
                stato_a, corpo_a = api.chiama(
                    "POST", f"/api/tavolo/{codice}/dichiara", {"punti": punti}, sessione
                )
                azioni.append(
                    {
                        "partita": partita_corrente,
                        "mano": s["mano"]["numero"],
                        "chi": nome,
                        "mossa": "dichiara",
                        "valore": punti,
                        "verita": s["tuoiPunti"],
                        "status": stato_a,
                        "errore": corpo_a if stato_a != 200 else None,
                    }
                )
                agito = True
                break
            if s["puoiRispondere"]:
                mossa = scegli_risposta(rng, s)
                stato_a, corpo_a = api.chiama(
                    "POST", f"/api/tavolo/{codice}/{mossa}", {}, sessione
                )
                azioni.append(
                    {
                        "partita": partita_corrente,
                        "mano": s["mano"]["numero"],
                        "chi": nome,
                        "mossa": mossa,
                        "dichiarazione": s["mano"]["dichiarazione"],
                        "mioPunto": s["tuoiPunti"],
                        "status": stato_a,
                        "errore": corpo_a if stato_a != 200 else None,
                    }
                )
                agito = True
                break
        if agito:
            continue

        # ---- puntate ----
        for nome, sessione in umani:
            s = stati[nome]
            if not s["toccaATe"]:
                continue
            mossa, gettoni = scegli_mossa(rng, s, args.profilo)
            if mossa == "punta":
                stato_a, corpo_a = api.chiama(
                    "POST", f"/api/tavolo/{codice}/punta", {"gettoni": gettoni}, sessione
                )
            else:
                stato_a, corpo_a = api.chiama(
                    "POST", f"/api/tavolo/{codice}/{mossa}", {}, sessione
                )
            azioni.append(
                {
                    "partita": partita_corrente,
                    "mano": s["mano"]["numero"],
                    "chi": nome,
                    "mossa": mossa,
                    "gettoni": gettoni,
                    "massima": s["mano"]["puntataMassima"],
                    "status": stato_a,
                    "errore": corpo_a if stato_a != 200 else None,
                }
            )
            agito = True
            break
        if agito:
            continue

        # nessuno dei due umani puo' muovere: il tavolo e' bloccato
        controlla(
            esiti,
            "tavolo mai in stallo",
            False,
            f"mano {p['mano']['numero']} fase dichiarazione="
            f"{p['mano']['inDichiarazione']} toccaATe P={p['toccaATe']} "
            f"M={m['toccaATe']}",
        )
        giri.append(
            {
                "partita": partita_corrente,
                "mano": p["mano"]["numero"],
                "stallo": True,
                "Peppa": snap("Peppa", p),
                "Marta": snap("Marta", m),
            }
        )
        break

    # ---- controlli sulle carte raccolte, per partita ----
    for partita in sorted({chiave[0] for chiave in carte_per_mano}):
        for nome in ("Peppa", "Marta"):
            viste = [
                carta
                for (p, _mano, chi), carte in carte_per_mano.items()
                if p == partita and chi == nome
                for carta in carte
            ]
            controlla(
                esiti,
                f"carte di {nome} senza duplicati nella partita {partita}",
                len(viste) == len(set(viste)),
                f"{len(viste)} carte viste, {len(set(viste))} distinte",
            )
            attese = 8 if partita < partita_corrente else 6
            controlla(
                esiti,
                f"carte totali di {nome} nella partita {partita}",
                len(viste) == attese,
                f"{len(viste)} carte (attese {attese} con 5 giocatori)",
            )
        condivise = []
        for mano in sorted({chiave[1] for chiave in carte_per_mano if chiave[0] == partita}):
            p_carte = set(carte_per_mano.get((partita, mano, "Peppa"), []))
            m_carte = set(carte_per_mano.get((partita, mano, "Marta"), []))
            condivise.extend(p_carte & m_carte)
        controlla(
            esiti,
            f"Peppa e Marta non hanno carte in comune nella partita {partita}",
            not condivise,
            f"carte condivise: {condivise}",
        )

    # ---- documento finale della prima partita ----
    if verbale_html:
        Path("docs/verbale-prova-5-giocatori.html").write_text(
            verbale_html, encoding="utf-8"
        )

    # ---- verifica dichiarazioni e punti dagli stati registrati ----
    for giro in giri:
        mano = giro["stato_mano"]
        if not mano["senzaPuntate"] and mano["carteVincenti"]:
            atteso = punti_mano(mano["carteVincenti"])
            controlla(
                esiti,
                f"punti del vincitore coerenti (mano {mano['numero']})",
                mano["puntiVincenti"] == atteso,
                f"{mano['puntiVincenti']} vs {atteso}",
            )

    risultato = {
        "meta": {
            "base": args.base,
            "codice": codice,
            "seed": args.seed,
            "profilo": args.profilo,
            "nomi": nomi,
            "mani_giocate": totale_mani_giocate,
            "partite": partita_corrente,
            "snapshot_duo": snapshot_duo,
        },
        "esiti": esiti,
        "azioni": azioni,
        "giri": giri,
        "prove": prove,
        "problemi": [e for e in esiti if not e["ok"]],
    }
    Path(args.out).write_text(
        json.dumps(risultato, indent=2, ensure_ascii=False), encoding="utf-8"
    )
    print(
        f"\n{len(risultato['problemi'])} problemi su {len(esiti)} controlli; "
        f"{totale_mani_giocate} mani in {partita_corrente} partite"
    )


if __name__ == "__main__":
    main()
