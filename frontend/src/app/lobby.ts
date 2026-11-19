import { Component, computed, input, output, signal } from '@angular/core';

type Posto = { tipo: 'umano' | 'bot' | 'libero'; nome: string };

const POSTI = 8;

@Component({
  selector: 'app-lobby',
  template: `
    <section class="lobby">
      <header class="testata">
        <p class="occhiello">Lobby</p>
        <h2>Stanza <span class="codice">{{ codice }}</span></h2>
        <p class="testo">
          Tocca un posto libero per far sedere un bot, oppure passa il codice a chi vuoi invitare.
        </p>
      </header>

      <div class="posti">
        @for (posto of posti(); track $index; let i = $index) {
          <button
            type="button"
            class="posto"
            [class.occupato]="posto.tipo !== 'libero'"
            [class.mio]="i === 0"
            (click)="cambiaPosto(i)"
          >
            <span class="posto-numero">{{ i + 1 }}</span>
            @if (posto.tipo === 'libero') {
              <span class="posto-nome">Libero</span>
              <span class="posto-azione">+</span>
            } @else {
              <span class="posto-nome">{{ i === 0 ? nome() : posto.nome }}</span>
              <span class="posto-azione">{{ i === 0 ? 'tu' : 'bot' }}</span>
            }
          </button>
        }
      </div>

      <footer class="azioni">
        <button type="button" class="ghost" (click)="indietro.emit()">Indietro</button>
        <span class="conteggio">
          <span class="ficha oro piccola"></span>
          {{ occupati() }} / {{ totali }} al tavolo
        </span>
        <button type="button" class="cta" [disabled]="occupati() < 4" (click)="avvia()">
          Inizia la partita
        </button>
      </footer>

      @if (occupati() < 4) {
        <p class="nota">Servono almeno quattro giocatori: aggiungi qualche bot per iniziare subito.</p>
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

      .posto:hover {
        border-color: rgba(216, 169, 74, 0.5);
        transform: translateY(-1px);
      }

      .posto.occupato {
        border-style: solid;
        border-color: rgba(246, 239, 223, 0.14);
        background: linear-gradient(180deg, rgba(9, 44, 31, 0.9), rgba(3, 20, 14, 0.9));
        color: var(--crema);
      }

      .posto.mio {
        border-color: rgba(216, 169, 74, 0.65);
        box-shadow: inset 0 0 0 1px rgba(216, 169, 74, 0.25);
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

      @media (max-width: 560px) {
        .azioni .cta {
          width: 100%;
        }
      }
    `,
  ],
})
export class Lobby {
  readonly inizia = output<string>();

  readonly indietro = output<void>();

  readonly nome = input<string>('Tu');

  protected readonly totali = POSTI;

  protected readonly codice = codiceStanza();

  protected readonly posti = signal<Posto[]>(
    Array.from({ length: POSTI }, (_, indice) =>
      indice === 0 ? { tipo: 'umano' as const, nome: 'Tu' } : { tipo: 'libero' as const, nome: '' },
    ),
  );

  protected readonly occupati = computed(() => this.posti().filter((posto) => posto.tipo !== 'libero').length);

  protected cambiaPosto(indice: number): void {
    if (indice === 0) {
      return;
    }
    this.posti.update((posti) => {
      const copia = [...posti];
      const posto = copia[indice];
      copia[indice] =
        posto.tipo === 'libero'
          ? { tipo: 'bot', nome: `Bot ${copia.filter((p) => p.tipo === 'bot').length + 1}` }
          : { tipo: 'libero', nome: '' };
      return copia;
    });
  }

  protected avvia(): void {
    if (this.occupati() >= 4) {
      this.inizia.emit(this.codice);
    }
  }
}

function codiceStanza(): string {
  const lettere = 'ABCDEFGHJKLMNPQRSTUVWXYZ';
  let codice = '';
  for (let i = 0; i < 4; i++) {
    codice += lettere.charAt(Math.floor(Math.random() * lettere.length));
  }
  return codice;
}
