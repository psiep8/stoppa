import { Component, OnDestroy, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { CanaleStanza } from './canale-stanza';
import { coloreDi } from './colori';
import { Carta, StatoTavolo, chiamataApi, etichettaSeme, etichettaValore } from './gioco';
import { PiattoFiches } from './piatto-fiches';
import { PilaFiches } from './pila-fiches';
import { PuntateTavolo } from './puntate-tavolo';
import { Suoni } from './suoni';
import { Vista } from './vista';

@Component({
  selector: 'app-tavolo',
  templateUrl: './tavolo.html',
  styleUrl: './tavolo.css',
  imports: [PilaFiches, PiattoFiches, PuntateTavolo],
})
export class Tavolo implements OnInit, OnDestroy {
  readonly esci = output<void>();

  readonly nome = input<string>('Tu');

  readonly codice = input.required<string>();

  readonly sessione = input.required<string>();

  private readonly vista = inject(Vista);

  private readonly suoni = inject(Suoni);

  private readonly canale = inject(CanaleStanza);

  protected readonly stato = signal<StatoTavolo | null>(null);

  protected readonly errore = signal<string | null>(null);

  protected readonly attesa = signal(false);

  protected readonly puntata = signal(1);

  /** Punti da dichiarare: si scelgono con i bottoni o si scrivono. */
  protected readonly dichiarazione = signal(20);

  /** La vista POV: il tavolo davanti e le carte in mano, anche da PC. */
  protected readonly pov = this.vista.pov;

  protected readonly alzate = this.vista.alzate;

  private partenzaTocco?: number;

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

  /** Il posto di chi ha vinto la mano: le fiches puntate volano lì. */
  protected readonly vincitoreIndice = computed(() => {
    const stato = this.stato();
    if (!stato || !stato.mano.chiusa || !stato.mano.vincitori.length) {
      return -1;
    }
    const nome = stato.mano.vincitori[0];
    return stato.giocatori.find((giocatore) => giocatore.nome === nome)?.indice ?? -1;
  });

  ngOnInit(): void {
    this.vista.inGioco.set(true);
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
    this.vista.inGioco.set(false);
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

  protected iniziaGesto(evento: PointerEvent): void {
    this.partenzaTocco = evento.clientY;
  }

  /** Giù per abbassare le carte e vedere il tavolo, su per riprenderle. */
  protected finisciGesto(evento: PointerEvent): void {
    if (this.partenzaTocco === undefined) {
      return;
    }
    const scarto = evento.clientY - this.partenzaTocco;
    this.partenzaTocco = undefined;
    if (scarto > 30) {
      this.alzate.set(false);
    } else if (scarto < -30) {
      this.alzate.set(true);
    }
  }

  protected colore(indice: number): string {
    return coloreDi(indice);
  }

  /** Un gettone in più, con il rumore della ficha che si posa. */
  protected piu(): void {
    const stato = this.stato();
    if (!stato || this.puntata() >= stato.puntataMassima) {
      return;
    }
    this.puntata.set(this.puntata() + 1);
    this.suoni.fiche();
  }

  protected meno(): void {
    const stato = this.stato();
    if (!stato || this.puntata() <= stato.puntataMinima) {
      return;
    }
    this.puntata.set(this.puntata() - 1);
    this.suoni.fiche();
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

  protected piuDichiarazione(): void {
    this.dichiarazione.update((punti) => Math.min(55, punti + 1));
  }

  protected menoDichiarazione(): void {
    this.dichiarazione.update((punti) => Math.max(1, punti - 1));
  }

  protected scriviDichiarazione(evento: Event): void {
    const valore = Number((evento.target as HTMLInputElement).value);
    this.dichiarazione.set(Number.isFinite(valore) ? Math.min(55, Math.max(1, Math.round(valore))) : 1);
  }

  protected async dichiara(): Promise<void> {
    if (!this.inviaMossa({ tipo: 'dichiara', gettoni: this.dichiarazione() })) {
      await this.azione('/dichiara', { punti: this.dichiarazione() });
    }
  }

  protected async contesta(): Promise<void> {
    if (!this.inviaMossa({ tipo: 'contesta' })) {
      await this.azione('/contesta', {});
    }
  }

  protected async accetta(): Promise<void> {
    if (!this.inviaMossa({ tipo: 'accetta' })) {
      await this.azione('/accetta', {});
    }
  }

  /** Scelta personale: far vedere le proprie fiches, o tornare a coprirle. */
  protected async mostraFiches(): Promise<void> {
    const mostra = !(this.io()?.fichesInVista ?? false);
    this.suoni.fiche();
    if (!this.inviaMossa({ tipo: 'mostra-fiches', mostra })) {
      await this.azione('/mostra-fiches', { mostra });
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

  /** Il documento di fine partita: HTML da esportare, con i gettoni di ognuno. */
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
    const prima = this.precedente;
    // la POV abbassa le carte quando il tavolo ha qualcosa da leggere (esito o
    // dichiarazione) e le rialza sulla mano nuova
    const daLeggere = nuovo.mano.chiusa || nuovo.mano.inDichiarazione;
    if (prima && daLeggere && !(prima.mano.chiusa || prima.mano.inDichiarazione)) {
      this.vista.alzate.set(false);
    } else if (prima && nuovo.mano.numero !== prima.mano.numero) {
      this.vista.alzate.set(true);
    }
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

