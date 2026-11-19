import { Injectable } from '@angular/core';

/**
 * Effetti sonori sintetizzati con la Web Audio API: niente file da scaricare,
 * nessun asset da attribuire. L'AudioContext nasce al primo gesto dell'utente.
 */
@Injectable({ providedIn: 'root' })
export class Suoni {
  private contesto?: AudioContext;

  private rumore?: AudioBuffer;

  private silenzio = false;

  attivo(): boolean {
    return !this.silenzio;
  }

  impostaAttivo(attivo: boolean): void {
    this.silenzio = !attivo;
    if (attivo) {
      this.sblocca();
    }
  }

  /** Da chiamare su un click: i browser non fanno partire l'audio prima. */
  sblocca(): void {
    if (!this.contesto) {
      const costruzione = window.AudioContext ?? (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
      if (!costruzione) {
        return;
      }
      this.contesto = new costruzione();
    }
    void this.contesto.resume();
  }

  /** Bussare sul tavolo: si passa. */
  passata(): void {
    this.rumoreBreve(0, 0.07, 380, 'lowpass', 0.22);
    this.rumoreBreve(0.11, 0.07, 340, 'lowpass', 0.2);
    this.tono(115, 0.1, 0.07);
  }

  /** Mazzo che si distribuisce. */
  carteDistribuite(): void {
    for (let i = 0; i < 7; i++) {
      this.rumoreBreve(i * 0.06, 0.055, 2300 + Math.random() * 1400, 'bandpass', 0.09);
    }
  }

  /** Carte girate al confronto. */
  swishCarte(): void {
    for (let i = 0; i < 5; i++) {
      this.rumoreBreve(i * 0.045, 0.14, 1500 + i * 620, 'bandpass', 0.11);
    }
  }

  /** Fiche trascinate o puntate. */
  fiche(): void {
    this.tono(1760, 0.09, 0.045);
    this.tono(2440, 0.07, 0.03, 0.012);
    this.rumoreBreve(0, 0.05, 5200, 'highpass', 0.05);
  }

  /** Monete che cadono nel piatto. */
  piatto(): void {
    this.tono(1380, 0.12, 0.05);
    this.tono(2070, 0.1, 0.035, 0.06);
    this.rumoreBreve(0, 0.09, 4300, 'highpass', 0.07);
  }

  /** Chiusura di partita. */
  vittoria(): void {
    [523.25, 659.25, 783.99, 1046.5].forEach((frequenza, indice) =>
      this.tono(frequenza, 0.36, 0.055, indice * 0.09, 'triangle'),
    );
  }

  private tono(frequenza: number, durata: number, volume: number, ritardo = 0, forma: OscillatorType = 'sine'): void {
    const contesto = this.pronto();
    if (!contesto) {
      return;
    }
    const oscillatore = contesto.createOscillator();
    oscillatore.type = forma;
    oscillatore.frequency.value = frequenza;
    const guadagno = contesto.createGain();
    const inizio = contesto.currentTime + ritardo;
    guadagno.gain.setValueAtTime(0, inizio);
    guadagno.gain.linearRampToValueAtTime(volume, inizio + 0.005);
    guadagno.gain.exponentialRampToValueAtTime(0.0001, inizio + durata);
    oscillatore.connect(guadagno).connect(contesto.destination);
    oscillatore.start(inizio);
    oscillatore.stop(inizio + durata + 0.03);
  }

  private rumoreBreve(
    ritardo: number,
    durata: number,
    frequenza: number,
    tipo: BiquadFilterType,
    volume: number,
  ): void {
    const contesto = this.pronto();
    if (!contesto) {
      return;
    }
    const sorgente = contesto.createBufferSource();
    sorgente.buffer = this.bufferRumore(contesto);
    const filtro = contesto.createBiquadFilter();
    filtro.type = tipo;
    filtro.frequency.value = frequenza;
    filtro.Q.value = 0.9;
    const guadagno = contesto.createGain();
    const inizio = contesto.currentTime + ritardo;
    guadagno.gain.setValueAtTime(0, inizio);
    guadagno.gain.linearRampToValueAtTime(volume, inizio + 0.004);
    guadagno.gain.exponentialRampToValueAtTime(0.0001, inizio + durata);
    sorgente.connect(filtro).connect(guadagno).connect(contesto.destination);
    sorgente.start(inizio);
    sorgente.stop(inizio + durata + 0.02);
  }

  private bufferRumore(contesto: AudioContext): AudioBuffer {
    if (!this.rumore) {
      const lunghezza = Math.floor(contesto.sampleRate);
      const buffer = contesto.createBuffer(1, lunghezza, contesto.sampleRate);
      const dati = buffer.getChannelData(0);
      for (let i = 0; i < lunghezza; i++) {
        dati[i] = Math.random() * 2 - 1;
      }
      this.rumore = buffer;
    }
    return this.rumore;
  }

  private pronto(): AudioContext | undefined {
    return this.silenzio ? undefined : this.contesto;
  }
}
