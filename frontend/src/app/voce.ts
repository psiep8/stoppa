import { Injectable, inject, signal } from '@angular/core';
import { AvvisoVoce, CanaleStanza, DatiVoce, PartecipanteVoce } from './canale-stanza';

type Rete = {
  pc: RTCPeerConnection;
  audio?: HTMLAudioElement;
  daConfigurare: RTCIceCandidateInit[];
  pronta: boolean;
};

/**
 * La voce della stanza: le tracce audio viaggiano da browser a browser, il
 * WebSocket della stanza fa solo da segnalazione (offerte, risposte, ICE).
 *
 * Chi accende il microfono entra nel roster del server e riceve un id opaco;
 * le offerte le manda solo il peer con l'id minore, così due giocatori non si
 * offrono la stessa connessione nello stesso momento.
 */
@Injectable({ providedIn: 'root' })
export class Voce {
  private readonly canale = inject(CanaleStanza);

  readonly attivo = signal(false);

  readonly microfono = signal(true);

  readonly partecipanti = signal<PartecipanteVoce[]>([]);

  readonly mio = signal('');

  /** Chi ho silenziato io, solo sul mio dispositivo. */
  readonly muti = signal<ReadonlySet<string>>(new Set());

  readonly errore = signal<string | null>(null);

  private flusso?: MediaStream;

  private reti = new Map<string, Rete>();

  constructor() {
    this.canale.ascoltaVoce((avviso) => this.ricevi(avviso));
    this.canale.ascoltaApertura(() => {
      if (this.attivo()) {
        this.canale.invia({ tipo: 'voce-entra' });
      }
    });
  }

  /** Accende il microfono e annuncia la presenza; il browser chiede il permesso. */
  async alterna(): Promise<void> {
    if (!this.attivo()) {
      await this.accendi();
      return;
    }
    const acceso = !this.microfono();
    this.microfono.set(acceso);
    this.flusso?.getAudioTracks().forEach((traccia) => (traccia.enabled = acceso));
    this.canale.invia({ tipo: 'voce-muta', muto: !acceso });
  }

  /** Silenzia o riascolta un giocatore, senza dirlo agli altri. */
  alternaMuto(id: string): void {
    const muti = new Set(this.muti());
    if (muti.has(id)) {
      muti.delete(id);
    } else {
      muti.add(id);
    }
    this.muti.set(muti);
    const audio = this.reti.get(id)?.audio;
    if (audio) {
      audio.muted = muti.has(id);
    }
  }

  spegni(): void {
    if (this.attivo()) {
      this.canale.invia({ tipo: 'voce-esce' });
    }
    for (const id of [...this.reti.keys()]) {
      this.chiudiRete(id);
    }
    this.flusso?.getTracks().forEach((traccia) => traccia.stop());
    this.flusso = undefined;
    this.attivo.set(false);
    this.microfono.set(true);
    this.partecipanti.set([]);
    this.mio.set('');
    this.muti.set(new Set());
    this.errore.set(null);
  }

  private async accendi(): Promise<void> {
    if (this.attivo()) {
      return;
    }
    if (!navigator.mediaDevices?.getUserMedia) {
      this.errore.set('Il microfono richiede https o localhost.');
      return;
    }
    try {
      this.flusso = await navigator.mediaDevices.getUserMedia({
        audio: { echoCancellation: true, noiseSuppression: true, autoGainControl: true },
      });
    } catch {
      this.errore.set('Microfono non disponibile: controlla il permesso del browser.');
      return;
    }
    this.errore.set(null);
    this.attivo.set(true);
    this.microfono.set(true);
    if (!this.canale.invia({ tipo: 'voce-entra' })) {
      this.errore.set('Sei fuori dalla stanza: rientra per parlare.');
      this.spegni();
    }
  }

  private ricevi(avviso: AvvisoVoce): void {
    if ('roster' in avviso) {
      this.mio.set(avviso.roster.mio);
      this.partecipanti.set(avviso.roster.partecipanti);
      this.allinea(avviso.roster.partecipanti);
      return;
    }
    void this.dati(avviso.da, avviso.dati);
  }

  /** Accende le reti mancanti e chiude quelle di chi se n'è andato. */
  private allinea(partecipanti: PartecipanteVoce[]): void {
    const mio = this.mio();
    const presenti = new Set(partecipanti.map((partecipante) => partecipante.id));
    for (const id of [...this.reti.keys()]) {
      if (!presenti.has(id)) {
        this.chiudiRete(id);
      }
    }
    for (const partecipante of partecipanti) {
      if (partecipante.id !== mio && !this.reti.has(partecipante.id) && mio < partecipante.id) {
        void this.offri(partecipante.id);
      }
    }
  }

  private rete(id: string): Rete {
    const esistente = this.reti.get(id);
    if (esistente) {
      return esistente;
    }
    const pc = new RTCPeerConnection({ iceServers: [{ urls: 'stun:stun.l.google.com:19302' }] });
    const rete: Rete = { pc, daConfigurare: [], pronta: false };
    this.reti.set(id, rete);
    this.flusso?.getTracks().forEach((traccia) => pc.addTrack(traccia, this.flusso!));
    pc.onicecandidate = (evento) => {
      if (evento.candidate) {
        this.inviaDati(id, { genere: 'ice', candidato: evento.candidate.toJSON() });
      }
    };
    pc.ontrack = (evento) => this.riproduci(id, evento.streams[0]);
    pc.onconnectionstatechange = () => {
      if (pc.connectionState === 'failed') {
        pc.restartIce();
      }
    };
    return rete;
  }

  private async offri(id: string): Promise<void> {
    const rete = this.rete(id);
    try {
      await rete.pc.setLocalDescription(await rete.pc.createOffer());
    } catch {
      return;
    }
    const offerta = rete.pc.localDescription;
    if (offerta) {
      this.inviaDati(id, { genere: 'offerta', sdp: offerta });
    }
  }

  private async dati(da: string, dati: DatiVoce): Promise<void> {
    const rete = this.rete(da);
    if (dati.genere === 'ice') {
      if (rete.pronta) {
        await rete.pc.addIceCandidate(dati.candidato).catch(() => undefined);
      } else {
        rete.daConfigurare.push(dati.candidato);
      }
      return;
    }
    try {
      await rete.pc.setRemoteDescription(dati.sdp);
      rete.pronta = true;
    } catch {
      return;
    }
    await this.svuota(rete);
    if (dati.genere === 'risposta') {
      return;
    }
    await rete.pc.setLocalDescription(await rete.pc.createAnswer());
    const risposta = rete.pc.localDescription;
    if (risposta) {
      this.inviaDati(da, { genere: 'risposta', sdp: risposta });
    }
  }

  private async svuota(rete: Rete): Promise<void> {
    const candidati = rete.daConfigurare.splice(0);
    for (const candidato of candidati) {
      await rete.pc.addIceCandidate(candidato).catch(() => undefined);
    }
  }

  private riproduci(id: string, flusso: MediaStream): void {
    const rete = this.reti.get(id);
    if (!rete) {
      return;
    }
    if (!rete.audio) {
      const audio = document.createElement('audio');
      audio.autoplay = true;
      audio.style.display = 'none';
      document.body.appendChild(audio);
      rete.audio = audio;
    }
    rete.audio.muted = this.muti().has(id);
    rete.audio.srcObject = flusso;
    void rete.audio.play().catch(() => undefined);
  }

  private inviaDati(a: string, dati: DatiVoce): void {
    this.canale.invia({ tipo: 'voce', a, dati });
  }

  private chiudiRete(id: string): void {
    const rete = this.reti.get(id);
    if (!rete) {
      return;
    }
    rete.pc.close();
    if (rete.audio) {
      rete.audio.srcObject = null;
      rete.audio.remove();
    }
    this.reti.delete(id);
  }
}
