import { Component, OnDestroy, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { CanaleStanza } from './canale-stanza';
import { IngressoLobby, PostoLobby, StanzaLobby, StatoTavolo, inviaApi } from './gioco';

@Component({
  selector: 'app-lobby',
  template: `
    <section class="lobby">
      <header class="testata">
        <p class="occhiello">Lobby</p>
        <h2>
          @if (stanza(); as s) {
            Stanza <span class="codice">{{ s.codice }}</span>
          } @else {
            Stanza
          }
        </h2>
        <p class="testo">
          @if (avviata()) {
            Partita iniziata: si torna al tavolo.
          } @else {
            Tocca un posto libero per aggiungere un bot, tocca un bot per toglierlo.
          }
        </p>

        <form class="unisciti" (submit)="unisciti($event)">
          <input
            [value]="codiceIngresso()"
            (input)="aggiornaCodice($event)"
            maxlength="4"
            placeholder="Codice stanza"
            aria-label="Codice stanza"
            autocomplete="off"
          />
          <button type="submit" class="ghost" [disabled]="attesa() || codiceIngresso().trim().length !== 4">
            Entra
          </button>
        </form>
      </header>

      <div class="posti">
        @for (posto of posti(); track $index; let i = $index) {
          @if (posto) {
            <button
              type="button"
              class="posto occupato"
              [class.mio]="i === mio()"
              (click)="togli(i)"
              [disabled]="avviata() || i === mio()"
            >
              <span class="posto-numero">{{ i + 1 }}</span>
              <span class="posto-nome">{{ posto.nome }}</span>
              <span class="posto-azione">{{ ruolo(posto, i) }}</span>
            </button>
          } @else {
            <button type="button" class="posto libero" (click)="aggiungiBot()" [disabled]="attesa() || avviata()">
              <span class="posto-numero">{{ i + 1 }}</span>
              <span class="posto-nome">Libero</span>
              <span class="posto-azione">+</span>
            </button>
          }
        }
      </div>

      <footer class="azioni">
        <button type="button" class="ghost" (click)="indietro.emit()">Indietro</button>
        <span class="conteggio">
          <span class="ficha oro piccola"></span>
          {{ occupati() }} / {{ totali() }} al tavolo
        </span>
        @if (avviata()) {
          <button type="button" class="cta" (click)="tornaAlTavolo()">Torna al tavolo</button>
        } @else {
          <button
            type="button"
            class="cta"
            (click)="avvia()"
            [disabled]="attesa() || !stanza() || occupati() < minimo()"
          >
            Avvia partita
          </button>
        }
      </footer>

      @if (stanza() && !avviata() && occupati() < minimo()) {
        <p class="nota">Minimo {{ minimo() }} giocatori.</p>
      }

      @if (errore(); as messaggio) {
        <p class="errore">{{ messaggio }}</p>
      }
    </section>
  `,
  styles: [
    `
      .lobby {
        display: grid;
        gap: clamp(1rem, 3vw, 2rem);
        padding: clamp(1rem, 3vw, 2rem) 0;
      }

      .testata {
        display: grid;
        gap: 0.5rem;
        max-width: 40rem;
      }

      .testata h2 {
        margin: 0;
        font-size: clamp(1.6rem, 4vw, 2.4rem);
        letter-spacing: -0.01em;
        font-weight: 700;
      }

      .codice {
        display: inline-block;
        margin-left: 0.4rem;
        padding: 0.1rem 0.6rem;
        border-radius: 10px;
        border: 1px dashed rgba(216, 169, 74, 0.55);
        color: var(--oro-chiaro);
        font-variant-numeric: tabular-nums;
        letter-spacing: 0.14em;
      }

      .unisciti {
        display: flex;
        flex-wrap: wrap;
        gap: 0.5rem;
        margin-top: 0.4rem;
        max-width: 22rem;
      }

      .unisciti input {
        flex: 1 1 8rem;
        padding: 0.62rem 0.85rem;
        border-radius: 12px;
        border: 1px solid var(--bordo);
        background: rgba(3, 20, 14, 0.65);
        color: var(--crema);
        letter-spacing: 0.18em;
        text-transform: uppercase;
      }

      .unisciti input:focus {
        outline: none;
        border-color: rgba(216, 169, 74, 0.7);
        box-shadow: 0 0 0 3px rgba(216, 169, 74, 0.16);
      }

      .unisciti input::placeholder {
        letter-spacing: 0.04em;
        text-transform: none;
        color: rgba(246, 239, 223, 0.32);
      }

      .posti {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
        gap: 0.7rem;
      }

      .posto {
        display: grid;
        grid-template-columns: auto 1fr auto;
        align-items: center;
        gap: 0.6rem;
        padding: 0.9rem 1rem;
        min-height: 74px;
        border-radius: 16px;
        border: 1px dashed rgba(246, 239, 223, 0.18);
        background: rgba(4, 24, 16, 0.5);
        color: rgba(246, 239, 223, 0.55);
        text-align: left;
        transition: border-color 0.2s ease, background 0.2s ease, transform 0.2s var(--molla);
      }

      .posto:hover:not(:disabled) {
        border-color: rgba(216, 169, 74, 0.5);
        transform: translateY(-1px);
      }

      .posto.occupato {
        border-style: solid;
        border-color: rgba(246, 239, 223, 0.14);
        background: linear-gradient(180deg, rgba(9, 44, 31, 0.9), rgba(3, 20, 14, 0.9));
        color: var(--crema);
        cursor: pointer;
      }

      .posto.mio {
        border-color: rgba(216, 169, 74, 0.65);
        box-shadow: inset 0 0 0 1px rgba(216, 169, 74, 0.25);
        cursor: default;
      }

      .posto-numero {
        display: grid;
        place-items: center;
        width: 26px;
        height: 26px;
        border-radius: 50%;
        background: rgba(246, 239, 223, 0.08);
        font-size: 0.72rem;
        font-variant-numeric: tabular-nums;
      }

      .posto-nome {
        font-weight: 600;
        font-size: 0.95rem;
      }

      .posto-azione {
        font-size: 0.66rem;
        letter-spacing: 0.2em;
        text-transform: uppercase;
        color: rgba(246, 239, 223, 0.45);
      }

      .azioni {
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        justify-content: space-between;
        gap: 0.8rem;
        padding-top: 0.4rem;
      }

      .conteggio {
        display: inline-flex;
        align-items: center;
        gap: 0.5rem;
        font-size: 0.82rem;
        color: rgba(246, 239, 223, 0.65);
      }

      .errore {
        margin: 0;
        padding: 0.7rem 0.9rem;
        border-radius: 12px;
        border: 1px solid rgba(248, 113, 113, 0.45);
        background: rgba(127, 29, 29, 0.4);
        color: #fecaca;
        font-size: 0.82rem;
      }

      @media (max-width: 560px) {
        .azioni .cta {
          width: 100%;
        }
      }
    `,
  ],
})
export class Lobby implements OnInit, OnDestroy {
  private readonly canale = inject(CanaleStanza);

  private stacca?: () => void;

  readonly inizia = output<{ codice: string; sessione: string }>();

  readonly ingresso = output<{ codice: string; sessione: string }>();

  readonly indietro = output<void>();

  readonly nome = input<string>('Tu');

  readonly codiceIniziale = input<string>('');

  readonly sessioneIniziale = input<string>('');

  protected readonly stanza = this.canale.stanza;

  protected readonly sessione = signal('');

  protected readonly codiceIngresso = signal('');

  protected readonly errore = signal<string | null>(null);

  protected readonly attesa = signal(false);

  protected readonly posti = computed(() => {
    const stanza = this.stanza();
    const perIndice = new Map<number, PostoLobby>();
    stanza?.posti.forEach((posto) => perIndice.set(posto.indice, posto));
    return Array.from({ length: stanza?.postiTotali ?? 8 }, (_, indice) => perIndice.get(indice) ?? null);
  });

  protected readonly occupati = computed(() => this.stanza()?.posti.length ?? 0);

  protected readonly totali = computed(() => this.stanza()?.postiTotali ?? 8);

  protected readonly minimo = computed(() => this.stanza()?.minimoGiocatori ?? 4);

  protected readonly mio = computed(() => this.stanza()?.tuoIndice ?? -1);

  protected readonly avviata = computed(() => this.stanza()?.avviata ?? false);

  ngOnInit(): void {
    // la sessione persa la gestisce App: qui si mostra solo il resto
    this.stacca = this.canale.ascolta((avviso) => {
      if (avviso.tipo === 'errore' && avviso.motivo !== 'sessione') {
        this.errore.set(avviso.messaggio);
      }
    });
    const codice = this.codiceIniziale();
    const sessione = this.sessioneIniziale();
    if (codice && sessione) {
      this.sessione.set(sessione);
      this.canale.apri(codice, sessione);
      void this.carica(codice);
    } else {
      void this.crea();
    }
  }

  ngOnDestroy(): void {
    this.stacca?.();
  }

  protected ruolo(posto: PostoLobby, indice: number): string {
    if (posto.bot) {
      return 'bot';
    }
    return indice === this.mio() ? 'tu' : 'giocatore';
  }

  protected aggiornaCodice(evento: Event): void {
    this.codiceIngresso.set((evento.target as HTMLInputElement).value.toUpperCase().slice(0, 4));
  }

  protected unisciti(evento: Event): void {
    evento.preventDefault();
    const codice = this.codiceIngresso().trim();
    if (codice.length === 4) {
      void this.entra(codice);
    }
  }

  protected aggiungiBot(): void {
    const codice = this.stanza()?.codice;
    if (!codice) {
      return;
    }
    void this.eseguiStanza(() =>
      inviaApi<StanzaLobby>(`/api/lobby/${codice}/bot`, 'POST', undefined, this.sessione()),
    );
  }

  protected togli(indice: number): void {
    const codice = this.stanza()?.codice;
    if (!codice || indice === this.mio()) {
      return;
    }
    void this.eseguiStanza(() =>
      inviaApi<StanzaLobby>(`/api/lobby/${codice}/posti/${indice}`, 'DELETE', undefined, this.sessione()),
    );
  }

  protected avvia(): void {
    const codice = this.stanza()?.codice;
    if (!codice || this.occupati() < this.minimo()) {
      return;
    }
    this.attesa.set(true);
    this.errore.set(null);
    inviaApi<StatoTavolo>(`/api/lobby/${codice}/avvia`, 'POST', undefined, this.sessione())
      .then(() => this.inizia.emit({ codice, sessione: this.sessione() }))
      .catch((eccezione) => this.errore.set(messaggio(eccezione)))
      .finally(() => this.attesa.set(false));
  }

  protected tornaAlTavolo(): void {
    const codice = this.stanza()?.codice;
    if (codice) {
      this.inizia.emit({ codice, sessione: this.sessione() });
    }
  }

  private async crea(): Promise<void> {
    await this.eseguiIngresso(() => inviaApi<IngressoLobby>('/api/lobby', 'POST', { nome: this.nome() }));
  }

  private async entra(codice: string): Promise<void> {
    await this.eseguiIngresso(() =>
      inviaApi<IngressoLobby>(`/api/lobby/${codice}/entra`, 'POST', { nome: this.nome() }),
    );
  }

  private async carica(codice: string): Promise<void> {
    this.attesa.set(true);
    this.errore.set(null);
    try {
      this.canale.assorbi(
        await inviaApi<StanzaLobby>(`/api/lobby/${codice}`, 'GET', undefined, this.sessione()),
      );
    } catch (eccezione) {
      this.errore.set(messaggio(eccezione));
    } finally {
      this.attesa.set(false);
    }
  }

  private async eseguiIngresso(azione: () => Promise<IngressoLobby>): Promise<void> {
    this.attesa.set(true);
    this.errore.set(null);
    try {
      const ingresso = await azione();
      this.sessione.set(ingresso.sessione);
      this.canale.assorbi(ingresso.stanza);
      this.canale.apri(ingresso.stanza.codice, ingresso.sessione);
      this.ingresso.emit({ codice: ingresso.stanza.codice, sessione: ingresso.sessione });
      this.codiceIngresso.set('');
    } catch (eccezione) {
      this.errore.set(messaggio(eccezione));
    } finally {
      this.attesa.set(false);
    }
  }

  private async eseguiStanza(azione: () => Promise<StanzaLobby>): Promise<void> {
    this.attesa.set(true);
    this.errore.set(null);
    try {
      this.canale.assorbi(await azione());
    } catch (eccezione) {
      this.errore.set(messaggio(eccezione));
    } finally {
      this.attesa.set(false);
    }
  }
}

function messaggio(eccezione: unknown): string {
  return eccezione instanceof Error ? eccezione.message : 'Errore di rete';
}
