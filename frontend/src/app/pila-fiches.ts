import { Component, computed, input } from '@angular/core';

/**
 * Le fiches come oggetti: una pila di dischi con il conto accanto.
 * Il numero non sta mai sopra la ficha, si legge a fianco come su un tavolo.
 */
@Component({
  selector: 'app-pila',
  template: `
    <span
      class="pila"
      [class.piccola]="piccola()"
      [class.coperte]="coperte()"
      [attr.aria-label]="coperte() ? 'fiches coperte' : quantita() + ' fiches'"
      [attr.title]="coperte() ? 'fiches coperte' : null"
    >
      <span class="dischi">
        @for (disco of dischi(); track disco) {
          <span [class]="'disco ficha ' + (coperte() ? 'nero' : colore())"></span>
        }
      </span>
      @if (!coperte()) {
        <span class="conto">{{ quantita() }}</span>
      }
    </span>
  `,
  styles: [
    `
      .pila {
        display: inline-flex;
        align-items: center;
        gap: 0.3rem;
        vertical-align: middle;
      }

      .dischi {
        display: inline-flex;
        align-items: center;
      }

      .pila .disco {
        width: 22px;
        height: 22px;
        margin-left: -11px;
      }

      .pila .disco:first-child {
        margin-left: 0;
      }

      .piccola .disco {
        width: 17px;
        height: 17px;
        margin-left: -9px;
      }

      .conto {
        font-variant-numeric: tabular-nums;
        font-weight: 600;
        font-size: 0.84rem;
        line-height: 1;
      }

      .piccola .conto {
        font-size: 0.72rem;
      }

      /* da PC le fiches si vedono da lontano, come su un tavolo vero */
      @media (min-width: 900px) {
        .pila .disco {
          width: 30px;
          height: 30px;
          margin-left: -15px;
        }

        .pila .disco:first-child {
          margin-left: 0;
        }

        .piccola .disco {
          width: 24px;
          height: 24px;
          margin-left: -12px;
        }

        .conto {
          font-size: 0.98rem;
        }

        .piccola .conto {
          font-size: 0.84rem;
        }
      }
    `,
  ],
})
export class PilaFiches {
  readonly quantita = input.required<number>();

  readonly colore = input('oro');

  readonly piccola = input(false);

  /** Fiches coperte: chi non le mostra tiene la pila scoperta ma senza conto. */
  readonly coperte = input(false);

  /** Una pila non deve diventare una torre: si mostra il mucchio, il conto dice il resto. */
  protected readonly dischi = computed(() => {
    const quanti = this.coperte()
      ? 3
      : Math.max(0, Math.min(this.quantita(), this.piccola() ? 5 : 9));
    return Array.from({ length: quanti }, (_, indice) => indice);
  });
}
