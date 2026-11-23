import { Component, ElementRef, computed, effect, input, signal, viewChildren } from '@angular/core';
import { coloreDi } from './colori';
import { GiocatoreTavolo } from './gioco';

/**
 * Le puntate sul panno: una pila 3D per giocatore, con il numero esatto di
 * fiches messe. Ogni ficha nuova arriva lanciata e si posa sul manto; a fine
 * mano tutte le pile volano verso chi ha vinto.
 */
@Component({
  selector: 'app-puntate',
  host: { '[class.compatta]': 'compatta()' },
  template: `
    <div class="puntate">
      @for (g of inGioco(); track g.indice) {
        <div
          #cluster
          class="puntata"
          [attr.data-indice]="g.indice"
          [class.mia]="g.indice === tuoIndice()"
          [class.raccolta]="raccogliendo()"
          [style.--dx]="(spostamenti()[g.indice]?.x ?? 0) + 'px'"
          [style.--dy]="(spostamenti()[g.indice]?.y ?? 0) + 'px'"
        >
          <span class="puntata-nome">{{ g.nome }}</span>
          <span class="pila3d">
            @for (d of dischi(g.puntata); track d) {
              <span
                [class]="'ficha ' + colore(g.indice)"
                [style.--i]="d"
                [style.--giro]="giri(g.indice, d) + 'deg'"
              ></span>
            }
          </span>
          <span class="puntata-conto">{{ g.puntata }}</span>
        </div>
      }
    </div>
  `,
  styles: [
    `
      :host {
        display: block;
      }

      .puntate {
        display: flex;
        flex-wrap: wrap;
        justify-content: center;
        align-items: flex-end;
        gap: 0.35rem 0.9rem;
        perspective: 700px;
      }

      .puntata {
        display: grid;
        justify-items: center;
        gap: 1px;
        transform-style: preserve-3d;
        transition:
          transform 0.8s cubic-bezier(0.4, 0.1, 0.2, 1),
          opacity 0.25s 0.62s;
      }

      .puntata.raccolta {
        transform: translate(var(--dx, 0), var(--dy, 0)) scale(0.82);
        opacity: 0;
      }

      .puntata-nome {
        font-size: 0.55rem;
        letter-spacing: 0.12em;
        text-transform: uppercase;
        color: rgba(242, 232, 213, 0.6);
      }

      .puntata.mia .puntata-nome {
        color: var(--oro-chiaro);
      }

      .pila3d {
        position: relative;
        display: block;
        width: 34px;
        height: 44px;
        transform-style: preserve-3d;
      }

      .pila3d .ficha {
        position: absolute;
        left: 50%;
        bottom: 0;
        width: 30px;
        height: 30px;
        margin-left: -15px;
        transform: rotateX(58deg) translateZ(calc(var(--i, 0) * 3.2px)) rotateZ(var(--giro, 0deg));
        animation: getta 0.62s cubic-bezier(0.2, 0.8, 0.25, 1) backwards;
        animation-delay: calc(var(--i, 0) * 26ms);
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.4);
      }

      .puntata-conto {
        font-size: 0.62rem;
        font-weight: 600;
        font-variant-numeric: tabular-nums;
        color: var(--oro-chiaro);
      }

      /* POV: pila più piccola e nomi compatti */
      :host(.compatta) .puntata-nome {
        font-size: 0.5rem;
      }

      :host(.compatta) .pila3d {
        width: 26px;
        height: 32px;
      }

      :host(.compatta) .pila3d .ficha {
        width: 22px;
        height: 22px;
        margin-left: -11px;
      }

      :host(.compatta) .puntata-conto {
        font-size: 0.56rem;
      }

      /* il lancio: arriva dall'alto ruotando e si posa sul panno */
      @keyframes getta {
        0% {
          opacity: 0;
          transform: rotateX(58deg) translateZ(170px) rotateZ(calc(var(--giro, 0deg) + 180deg)) scale(0.7);
        }
        58% {
          opacity: 1;
          transform: rotateX(58deg) translateZ(calc(var(--i, 0) * 3.2px + 24px))
            rotateZ(calc(var(--giro, 0deg) + 320deg));
        }
        78% {
          transform: rotateX(58deg) translateZ(calc(var(--i, 0) * 3.2px - 3px)) rotateZ(var(--giro, 0deg));
        }
        90% {
          transform: rotateX(58deg) translateZ(calc(var(--i, 0) * 3.2px + 2px)) rotateZ(var(--giro, 0deg));
        }
        100% {
          transform: rotateX(58deg) translateZ(calc(var(--i, 0) * 3.2px)) rotateZ(var(--giro, 0deg));
        }
      }
    `,
  ],
})
export class PuntateTavolo {
  readonly giocatori = input.required<GiocatoreTavolo[]>();

  readonly tuoIndice = input(0);

  readonly chiusa = input(false);

  readonly vincitore = input(-1);

  readonly compatta = input(false);

  protected readonly inGioco = computed(() => this.giocatori().filter((g) => g.puntata > 0));

  protected readonly spostamenti = signal<Record<number, { x: number; y: number }>>({});

  protected readonly raccogliendo = signal(false);

  private readonly clusters = viewChildren<ElementRef<HTMLElement>>('cluster');

  constructor() {
    effect(() => {
      const chiusa = this.chiusa();
      this.vincitore();
      if (!chiusa) {
        this.raccogliendo.set(false);
        this.spostamenti.set({});
        return;
      }
      // le pile esistono già: si misurano e poi partono verso il vincitore
      queueMicrotask(() => this.misura());
    });
  }

  protected colore(indice: number): string {
    return coloreDi(indice);
  }

  protected dischi(quante: number): number[] {
    return Array.from({ length: Math.min(quante, 10) }, (_, indice) => indice);
  }

  /** Ogni ficha ha la sua rotazione, diversa per giocatore e per posto nella pila. */
  protected giri(indice: number, disco: number): number {
    return (indice * 37 + disco * 53) % 360;
  }

  private misura(): void {
    const nodi = this.clusters();
    const vincitore = nodi.find((nodo) => Number(nodo.nativeElement.dataset['indice']) === this.vincitore());
    if (!vincitore) {
      return;
    }
    const centro = vincitore.nativeElement.getBoundingClientRect();
    const spostamenti: Record<number, { x: number; y: number }> = {};
    for (const nodo of nodi) {
      const indice = Number(nodo.nativeElement.dataset['indice']);
      const rettangolo = nodo.nativeElement.getBoundingClientRect();
      spostamenti[indice] = {
        x: centro.left + centro.width / 2 - (rettangolo.left + rettangolo.width / 2),
        y: centro.top + centro.height / 2 - (rettangolo.top + rettangolo.height / 2),
      };
    }
    this.spostamenti.set(spostamenti);
    requestAnimationFrame(() => this.raccogliendo.set(true));
  }
}
