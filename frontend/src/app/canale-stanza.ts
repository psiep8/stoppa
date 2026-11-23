import { Injectable, signal } from '@angular/core';
import { StanzaLobby, StatoTavolo } from './gioco';

export type AvvisoCanale =
  | { tipo: 'stato'; stato: StatoTavolo }
  | { tipo: 'errore'; messaggio: string; motivo?: string };

export type PartecipanteVoce = { id: string; nome: string; muto: boolean };

export type DatiVoce =
  | { genere: 'offerta' | 'risposta'; sdp: RTCSessionDescriptionInit }
  | { genere: 'ice'; candidato: RTCIceCandidateInit };

export type AvvisoVoce =
  | { tipo: 'voce'; roster: { mio: string; partecipanti: PartecipanteVoce[] } }
  | { tipo: 'voce'; da: string; dati: DatiVoce };

type Messaggio = {
  tipo?: string;
  stanza?: StanzaLobby;
  stato?: StatoTavolo;
  messaggio?: string;
  motivo?: string;
  mio?: string;
  partecipanti?: PartecipanteVoce[];
  da?: string;
  dati?: DatiVoce;
};

/**
 * Il canale della stanza: un solo WebSocket per stanza, condiviso da lobby e
 * partita. Il server manda la stanza quando cambia (ingressi, bot, avvio) e lo
 * stato filtrato dopo ogni mossa, quindi nessuno deve interrogare il server.
 */
@Injectable({ providedIn: 'root' })
export class CanaleStanza {
  readonly stanza = signal<StanzaLobby | null>(null);

  private socket?: WebSocket;

  private codice = '';

  private sessione = '';

  private riconnessione?: ReturnType<typeof setTimeout>;

  private ascoltatori = new Set<(avviso: AvvisoCanale) => void>();

  private ascoltatoriVoce = new Set<(avviso: AvvisoVoce) => void>();

  private ascoltatoriApertura = new Set<() => void>();

  /** Apre il canale della stanza; se e' gia' quello giusto chiede una risincronizzazione. */
  apri(codice: string, sessione: string): void {
    if (this.socket && this.codice === codice && this.sessione === sessione) {
      if (this.socket.readyState === WebSocket.OPEN) {
        this.invia({ tipo: 'entra', codice, sessione });
      }
      return;
    }
    this.chiudi();
    this.codice = codice;
    this.sessione = sessione;
    this.collega();
  }

  chiudi(): void {
    clearTimeout(this.riconnessione);
    this.riconnessione = undefined;
    const socket = this.socket;
    this.socket = undefined;
    this.codice = '';
    this.sessione = '';
    this.stanza.set(null);
    socket?.close();
  }

  /** Prende la stanza da una risposta REST, in attesa del messaggio del socket. */
  assorbi(stanza: StanzaLobby): void {
    this.stanza.set(stanza);
  }

  ascolta(ascoltatore: (avviso: AvvisoCanale) => void): () => void {
    this.ascoltatori.add(ascoltatore);
    return () => this.ascoltatori.delete(ascoltatore);
  }

  ascoltaVoce(ascoltatore: (avviso: AvvisoVoce) => void): () => void {
    this.ascoltatoriVoce.add(ascoltatore);
    return () => this.ascoltatoriVoce.delete(ascoltatore);
  }

  /** A ogni apertura del socket, riconnessioni comprese. */
  ascoltaApertura(ascoltatore: () => void): () => void {
    this.ascoltatoriApertura.add(ascoltatore);
    return () => this.ascoltatoriApertura.delete(ascoltatore);
  }

  /** Manda un messaggio sul canale; false se non e' aperto (si usa la REST). */
  invia(messaggio: object): boolean {
    const socket = this.socket;
    if (!socket || socket.readyState !== WebSocket.OPEN) {
      return false;
    }
    socket.send(JSON.stringify(messaggio));
    return true;
  }

  private collega(): void {
    const protocollo = location.protocol === 'https:' ? 'wss:' : 'ws:';
    const socket = new WebSocket(`${protocollo}//${location.host}/ws/lobby`);
    this.socket = socket;

    socket.onopen = () => {
      this.invia({ tipo: 'entra', codice: this.codice, sessione: this.sessione });
      this.ascoltatoriApertura.forEach((ascoltatore) => ascoltatore());
    };

    socket.onmessage = (evento) => this.ricevi(String(evento.data));

    socket.onclose = () => {
      if (this.socket !== socket) {
        return;
      }
      this.socket = undefined;
      if (this.codice) {
        this.riconnessione = setTimeout(() => this.collega(), 1500);
      }
    };

    socket.onerror = () => socket.close();
  }

  private ricevi(dato: string): void {
    try {
      const messaggio = JSON.parse(dato) as Messaggio;
      if (messaggio.tipo === 'stanza' && messaggio.stanza) {
        this.stanza.set(messaggio.stanza);
        return;
      }
      if (messaggio.tipo === 'voce') {
        const voce: AvvisoVoce | null =
          messaggio.mio && messaggio.partecipanti
            ? { tipo: 'voce', roster: { mio: messaggio.mio, partecipanti: messaggio.partecipanti } }
            : messaggio.da && messaggio.dati
              ? { tipo: 'voce', da: messaggio.da, dati: messaggio.dati }
              : null;
        if (voce) {
          this.ascoltatoriVoce.forEach((ascoltatore) => ascoltatore(voce));
        }
        return;
      }
      const avviso: AvvisoCanale | null =
        messaggio.tipo === 'stato' && messaggio.stato
          ? { tipo: 'stato', stato: messaggio.stato }
          : messaggio.tipo === 'errore'
            ? { tipo: 'errore', messaggio: messaggio.messaggio ?? 'Mossa non valida', motivo: messaggio.motivo }
            : null;
      if (avviso) {
        this.ascoltatori.forEach((ascoltatore) => ascoltatore(avviso));
      }
    } catch {
      // messaggio non riconosciuto: si ignora
    }
  }
}
