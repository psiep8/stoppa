import { Component, computed, input } from '@angular/core';
import { COLORI } from './colori';

type Sparsa = { i: number; x: number; y: number; r: number; s: number };

/**
 * Il piatto come su un tavolo vero: le fiches non sono una pila ordinata, sono
 * sparse a caso sul panno. La disposizione dipende dal codice della stanza,
 * quindi ogni partita ha la sua e resta la stessa per tutta la mano.
 */
@Component({
  selector: 'app-piatto',
  template: `
    <span class="piatto-fiches" [attr.aria-label]="quantita() + ' fiches sparse nel piatto'">
      @for (f of sparse(); track f.i) {
        <span
          [class]="'ficha ' + colore(f.i)"
          [style.left.%]="f.x"
          [style.top.%]="f.y"
          [style.--r]="f.r + 'deg'"
          [style.--s]="f.s"
          [style.--i]="f.i"
        ></span>
      }
      <span class="conto" aria-hidden="true">{{ quantita() }}</span>
    </span>
  `,
  styles: [
    `
      .piatto-fiches {
        position: relative;
        display: block;
        width: clamp(150px, 20vw, 200px);
        height: 82px;
      }

      .piatto-fiches .ficha {
        position: absolute;
        width: 24px;
        height: 24px;
        margin: -12px 0 0 -12px;
        transform: rotate(var(--r)) scale(var(--s));
        animation: caduta 0.42s cubic-bezier(0.2, 0.9, 0.3, 1.2) backwards;
        animation-delay: calc(var(--i) * 22ms);
        box-shadow:
          0 3px 5px rgba(0, 0, 0, 0.35),
          inset 0 0 0 1px rgba(0, 0, 0, 0.18);
      }

      .conto {
        position: absolute;
        right: -0.3rem;
        bottom: -0.4rem;
        padding: 0 0.2rem;
        font-size: 0.78rem;
        font-weight: 600;
        font-variant-numeric: tabular-nums;
        color: var(--crema, #f2e8d5);
        text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
      }

      @media (min-width: 900px) {
        .piatto-fiches {
          height: 96px;
        }

        .piatto-fiches .ficha {
          width: 30px;
          height: 30px;
          margin: -15px 0 0 -15px;
        }

        .conto {
          font-size: 0.92rem;
        }
      }
    `,
  ],
})
export class PiattoFiches {
  readonly quantita = input.required<number>();

  /** Codice stanza: da qui nasce la disposizione, una per partita. */
  readonly seme = input('');

  /** Il piatto è misto: le fiches di ognuno restano del colore di chi le ha messe. */
  protected colore(indice: number): string {
    return COLORI[indice % COLORI.length];
  }

  protected readonly sparse = computed<Sparsa[]>(() => {
    const quante = Math.max(0, Math.min(this.quantita(), 16));
    const caso = casuale(impronta(this.seme()));
    const posti: Sparsa[] = [];
    for (let i = 0; i < 16; i++) {
      const angolo = caso() * Math.PI * 2;
      const raggio = Math.sqrt(caso()) * 0.46;
      posti.push({
        i,
        x: 50 + Math.cos(angolo) * raggio * 100,
        y: 50 + Math.sin(angolo) * raggio * 76,
        r: Math.round(caso() * 80 - 40),
        s: 0.86 + caso() * 0.3,
      });
    }
    return posti.slice(0, quante);
  });
}

/** FNV-1a: un numero stabile dal codice della stanza. */
function impronta(testo: string): number {
  let valore = 2166136261;
  for (let i = 0; i < testo.length; i++) {
    valore ^= testo.charCodeAt(i);
    valore = Math.imul(valore, 16777619);
  }
  return valore >>> 0;
}

/** Mulberry32: numeri pseudo-casuali ma ripetibili. */
function casuale(seme: number): () => number {
  let stato = seme;
  return () => {
    stato = (stato + 0x6d2b79f5) | 0;
    let t = Math.imul(stato ^ (stato >>> 15), 1 | stato);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}
