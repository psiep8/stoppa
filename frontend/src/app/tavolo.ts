import { Component, OnDestroy, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { CanaleStanza } from './canale-stanza';
import { Carta, StatoTavolo, chiamataApi, etichettaSeme, etichettaValore } from './gioco';
import { Suoni } from './suoni';

@Component({
  selector: 'app-tavolo',
  templateUrl: './tavolo.html',
  styleUrl: './tavolo.css',
})
export class Tavolo implements OnInit, OnDestroy {
  readonly esci = output<void>();

  readonly nome = input<string>('Tu');

  readonly codice = input.required<string>();

  readonly sessione = input.required<string>();

  private readonly suoni = inject(Suoni);

  private readonly canale = inject(CanaleStanza);

  protected readonly stato = signal<StatoTavolo | null>(null);

  protected readonly errore = signal<string | null>(null);

  protected readonly attesa = signal(false);

  protected readonly puntata = signal(1);

  private precedente: StatoTavolo | null = null;

  private trascinamento?: { elemento: HTMLElement; x: number; sinistra: number };

  private stacca?: () => void;

  protected readonly io = computed(() => {
    const stato = this.stato();
    return stato ? stato.giocatori[stato.tuoIndice] : null;
  });

  protected readonly avversari = computed(() => {
    const stato = this.stato();
    return stato ? stato.giocatori.filter((giocatore) => giocatore.indice !== stato.tuoIndice) : [];
  });

  protected readonly costoVedo = computed(() => {
    const stato = this.stato();
    const io = this.io();
    return stato && io ? Math.max(0, stato.mano.puntataMassima - io.puntata) : 0;
  });

  protected readonly turnoNome = computed(
    () => this.stato()?.giocatori.find((giocatore) => giocatore.diTurno)?.nome ?? '',
  );

  ngOnInit(): void {
    this.stacca = this.canale.ascolta((avviso) => {
      this.attesa.set(false);
      if (avviso.tipo === 'stato') {
        this.applica(avviso.stato);
      } else {
        this.errore.set(avviso.messaggio);
      }
    });
    this.canale.apri(this.codice(), this.sessione());
    void this.carica();
  }

  ngOnDestroy(): void {
    this.stacca?.();
  }

  protected descrizione(carta: Carta): string {
    return `${etichettaValore(carta.valore)} di ${etichettaSeme(carta.seme)}`;
  }

  protected immagine(file: string): string {
    return `/assets/carte/${file}.svg`;
  }

  protected dorsi(quante: number): number[] {
    return Array.from({ length: quante }, (_, indice) => indice);
  }

  protected rotazione(indice: number): string {
    const carte = this.io()?.carte ?? [];
    const centro = (carte.length - 1) / 2;
    return `rotate(${(indice - centro) * 3.5}deg)`;
  }

  protected cambiaPuntata(evento: Event): void {
    this.puntata.set(Number((evento.target as HTMLInputElement).value));
  }

  protected async carica(): Promise<void> {
    await this.azione('');
  }

  protected async passa(): Promise<void> {
    if (!this.inviaMossa({ tipo: 'passa' })) {
      await this.azione('/passa', {});
    }
  }

  protected async vedo(): Promise<void> {
    if (!this.inviaMossa({ tipo: 'vedo' })) {
      await this.azione('/vedo', {});
    }
  }

  protected async punta(): Promise<void> {
    if (!this.inviaMossa({ tipo: 'punta', gettoni: this.puntata() })) {
      await this.azione('/punta', { gettoni: this.puntata() });
    }
  }

  protected async prossimaMano(): Promise<void> {
    if (!this.inviaMossa({ tipo: 'prossima-mano' })) {
      await this.azione('/prossima-mano', {});
    }
  }

  protected async nuovaPartita(): Promise<void> {
    if (!this.inviaMossa({ tipo: 'nuova-partita' })) {
      await this.azione('/nuova', {});
    }
  }

  protected iniziaTrascinamento(evento: PointerEvent): void {
    const elemento = evento.currentTarget as HTMLElement;
    this.trascinamento = { elemento, x: evento.clientX, sinistra: elemento.scrollLeft };
    elemento.setPointerCapture(evento.pointerId);
  }

  protected muoviTrascinamento(evento: PointerEvent): void {
    if (!this.trascinamento) {
      return;
    }
    const delta = evento.clientX - this.trascinamento.x;
    this.trascinamento.elemento.scrollLeft = this.trascinamento.sinistra - delta;
  }

  protected fineTrascinamento(evento: PointerEvent): void {
    if (!this.trascinamento) {
      return;
    }
    const elemento = this.trascinamento.elemento;
    this.trascinamento = undefined;
    if (elemento.hasPointerCapture(evento.pointerId)) {
      elemento.releasePointerCapture(evento.pointerId);
    }
  }

  /** Manda la mossa sul canale; false se non e' aperto (si usa la REST). */
  private inviaMossa(messaggio: object): boolean {
    if (!this.canale.invia(messaggio)) {
      return false;
    }
    this.errore.set(null);
    this.attesa.set(true);
    return true;
  }

  private async azione(percorso: string, corpo?: unknown): Promise<void> {
    this.attesa.set(true);
    this.errore.set(null);
    try {
      const stato = await chiamataApi<StatoTavolo>(
        `/api/tavolo/${this.codice()}${percorso}`,
        corpo,
        this.sessione(),
      );
      this.applica(stato);
    } catch (eccezione) {
      this.errore.set(eccezione instanceof Error ? eccezione.message : 'Errore di rete');
    } finally {
      this.attesa.set(false);
    }
  }

  /** Il verbale di fine partita: HTML da scaricare, con le fiches di ognuno. */
  protected scaricaVerbale(): void {
    const codice = this.codice();
    this.attesa.set(true);
    fetch(`/api/tavolo/${codice}/verbale`, { headers: { 'X-Sessione': this.sessione() } })
      .then((risposta) => {
        if (!risposta.ok) {
          throw new Error('verbale non disponibile');
        }
        return risposta.blob();
      })
      .then((documento) => {
        const url = URL.createObjectURL(documento);
        const link = document.createElement('a');
        link.href = url;
        link.download = `stoppa-${codice}.html`;
        link.click();
        URL.revokeObjectURL(url);
      })
      .catch(() => this.errore.set('Non riesco a preparare il verbale'))
      .finally(() => this.attesa.set(false));
  }

  private applica(nuovo: StatoTavolo): void {
    this.riproduciSuoni(this.precedente, nuovo);
    this.precedente = nuovo;
    this.stato.set(nuovo);
    if (this.puntata() < nuovo.puntataMinima || this.puntata() > nuovo.puntataMassima) {
      this.puntata.set(nuovo.puntataMinima);
    }
  }

  private riproduciSuoni(prima: StatoTavolo | null, dopo: StatoTavolo): void {
    if (!prima) {
      this.suoni.carteDistribuite();
      return;
    }
    if (dopo.mano.numero !== prima.mano.numero) {
      this.suoni.carteDistribuite();
    }
    if (dopo.mano.chiusa && !prima.mano.chiusa) {
      this.suoni.swishCarte();
    }
    if (dopo.mano.piatto < prima.mano.piatto) {
      this.suoni.piatto();
    }
    if (dopo.finita && !prima.finita) {
      this.suoni.vittoria();
    }
    for (const evento of dopo.eventi.filter((testo) => !prima.eventi.includes(testo))) {
      if (/passa/i.test(evento)) {
        this.suoni.passata();
      } else if (/punta|rilancia|vede/i.test(evento)) {
        this.suoni.fiche();
      }
    }
  }
}
