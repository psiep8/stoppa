import { Component, OnInit, computed, input, output, signal } from '@angular/core';
import { inviaApi } from './gioco';

type PostoLobby = { indice: number; nome: string; bot: boolean };

type StanzaLobby = {
  codice: string;
  posti: PostoLobby[];
  postiTotali: number;
  minimoGiocatori: number;
};

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
        <p class="testo">Tocca un posto libero per aggiungere un bot, tocca un bot per toglierlo.</p>
      </header>

      <div class="posti">
        @for (posto of posti(); track $index; let i = $index) {
          @if (posto) {
            <button type="button" class="posto occupato" [class.mio]="i === 0" (click)="togli(i)">
              <span class="posto-numero">{{ i + 1 }}</span>
              <span class="posto-nome">{{ posto.nome }}</span>
              <span class="posto-azione">{{ posto.bot ? 'bot' : 'tu' }}</span>
            </button>
          } @else {
            <button type="button" class="posto libero" (click)="aggiungiBot()" [disabled]="attesa()">
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
        <button
          type="button"
          class="cta"
          (click)="avvia()"
          [disabled]="attesa() || !stanza() || occupati() < minimo()"
        >
          Avvia partita
        </button>
      </footer>

      @if (stanza() && occupati() < minimo()) {
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
export class Lobby implements OnInit {
  readonly inizia = output<string>();

  readonly indietro = output<void>();

  readonly nome = input<string>('Tu');

  protected readonly stanza = signal<StanzaLobby | null>(null);

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

  ngOnInit(): void {
    void this.esegui(() => inviaApi<StanzaLobby>('/api/lobby', 'POST', { nome: this.nome() }));
  }

  protected aggiungiBot(): void {
    const codice = this.stanza()?.codice;
    if (!codice) {
      return;
    }
    void this.esegui(() => inviaApi<StanzaLobby>(`/api/lobby/${codice}/bot`, 'POST'));
  }

  protected togli(indice: number): void {
    const codice = this.stanza()?.codice;
    if (!codice || indice === 0) {
      return;
    }
    void this.esegui(() => inviaApi<StanzaLobby>(`/api/lobby/${codice}/posti/${indice}`, 'DELETE'));
  }

  protected avvia(): void {
    const codice = this.stanza()?.codice;
    if (!codice || this.occupati() < this.minimo()) {
      return;
    }
    this.attesa.set(true);
    this.errore.set(null);
    inviaApi(`/api/lobby/${codice}/avvia`, 'POST')
      .then(() => this.inizia.emit(codice))
      .catch((eccezione) => this.errore.set(eccezione instanceof Error ? eccezione.message : 'Errore di rete'))
      .finally(() => this.attesa.set(false));
  }

  private async esegui(azione: () => Promise<StanzaLobby>): Promise<void> {
    this.attesa.set(true);
    this.errore.set(null);
    try {
      this.stanza.set(await azione());
    } catch (eccezione) {
      this.errore.set(eccezione instanceof Error ? eccezione.message : 'Errore di rete');
    } finally {
      this.attesa.set(false);
    }
  }
}
