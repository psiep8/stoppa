import { Component, inject } from '@angular/core';
import { Vista } from './vista';
import { Voce } from './voce';

/** Microfono della stanza e silenzia-per-giocatore, in una riga sola. */
@Component({
  selector: 'app-voce',
  template: `
    <section class="voce" aria-label="Voce della stanza">
      <button
        type="button"
        class="ghost microfono"
        (click)="voce.alterna()"
        [attr.aria-pressed]="voce.attivo()"
        [attr.aria-label]="
          voce.attivo() ? (voce.microfono() ? 'Spegni il microfono' : 'Accendi il microfono') : 'Parla'
        "
      >
        @if (voce.attivo() && !voce.microfono()) {
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round">
            <path d="M4 4l16 16" />
            <path d="M9 9v3a3 3 0 0 0 4.5 2.6" />
            <path d="M15 10.5V6a3 3 0 0 0-5.6-1.5" />
            <path d="M6.5 11.5a5.5 5.5 0 0 0 8.3 4.7" />
            <path d="M12 18v3M9 21h6" />
          </svg>
        } @else {
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round">
            <rect x="9" y="3" width="6" height="11" rx="3" />
            <path d="M6 11.5a6 6 0 0 0 12 0" />
            <path d="M12 17.5V21M9 21h6" />
          </svg>
        }
        <span>
          @if (!voce.attivo()) {
            Parla
          } @else if (voce.microfono()) {
            Microfono acceso
          } @else {
            Microfono spento
          }
        </span>
      </button>

      @if (vista.inGioco()) {
        <button
          type="button"
          class="ghost vista-tavolo"
          (click)="vista.alterna()"
          [attr.aria-pressed]="vista.pov()"
        >
          {{ vista.pov() ? 'Tavolo' : 'Vista POV' }}
        </button>
      }

      @if (voce.attivo()) {
        <ul class="voci">
          @for (p of voce.partecipanti(); track p.id) {
            <li>
              <span class="voce-nome">{{ p.nome }}</span>
              @if (p.id === voce.mio()) {
                <span class="voce-stato">tu{{ p.muto ? ' · senza microfono' : '' }}</span>
              } @else {
                @if (p.muto) {
                  <span class="voce-stato">senza microfono</span>
                }
                <button type="button" class="ghost silenzia" (click)="voce.alternaMuto(p.id)">
                  {{ voce.muti().has(p.id) ? 'Riascolta' : 'Silenzia' }}
                </button>
              }
            </li>
          }
        </ul>
      }

      @if (voce.errore(); as messaggio) {
        <p class="voce-errore">{{ messaggio }}</p>
      }
    </section>
  `,
  styles: [
    `
      .voce {
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        gap: 0.35rem 1.1rem;
        padding: 0 0 0.7rem;
        border-bottom: 1px solid var(--bordo);
        margin-bottom: 0.9rem;
      }

      .microfono {
        display: inline-flex;
        align-items: center;
        gap: 0.45rem;
        padding: 0.4rem 0.7rem;
        font-size: 0.74rem;
        letter-spacing: 0.08em;
        text-transform: none;
      }

      .vista-tavolo {
        padding: 0.4rem 0.7rem;
        font-size: 0.74rem;
        letter-spacing: 0.08em;
        text-transform: none;
      }

      .vista-tavolo[aria-pressed='true'] {
        border-color: var(--oro);
        color: var(--oro-chiaro);
      }

      .microfono[aria-pressed='true'] {
        border-color: var(--oro);
        color: var(--oro-chiaro);
      }

      .voci {
        display: flex;
        flex-wrap: wrap;
        gap: 0.3rem 1.2rem;
        margin: 0;
        padding: 0;
        list-style: none;
      }

      .voci li {
        display: flex;
        align-items: center;
        gap: 0.45rem;
        font-size: 0.78rem;
      }

      .voce-nome {
        font-weight: 600;
      }

      .voce-stato {
        font-size: 0.64rem;
        letter-spacing: 0.12em;
        text-transform: uppercase;
        color: var(--testo-tenue);
      }

      .silenzia {
        padding: 0.2rem 0.45rem;
        font-size: 0.64rem;
        letter-spacing: 0.08em;
        text-transform: none;
        border-color: var(--bordo);
      }

      .voce-errore {
        flex-basis: 100%;
        margin: 0;
        font-size: 0.76rem;
        color: #e8a79a;
      }
    `,
  ],
})
export class VocePannello {
  protected readonly voce = inject(Voce);

  protected readonly vista = inject(Vista);
}
