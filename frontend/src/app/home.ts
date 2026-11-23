import { Component, input, linkedSignal, output, signal } from '@angular/core';
import { RichiestaAccesso } from './gioco';

@Component({
  selector: 'app-home',
  template: `
    <section class="home">
      <div class="intro">
        <ul class="dati-partita">
          <li><strong>4–8</strong><span>giocatori</span></li>
          <li><strong>3</strong><span>carte a testa</span></li>
          <li><strong>1–10</strong><span>gettoni a puntata</span></li>
        </ul>

        <form class="accesso" (submit)="invia($event)">
          <label class="campo">
            <span>Nome</span>
            <input
              [value]="nome()"
              (input)="aggiorna($event)"
              maxlength="14"
              placeholder="Nome del giocatore"
              autocomplete="off"
            />
          </label>

          <div class="codici">
            <label class="campo stretto">
              <span>Stanza</span>
              <input
                [value]="codice()"
                (input)="aggiornaCodice($event)"
                maxlength="4"
                placeholder="ABCD"
                aria-label="Codice stanza"
                autocomplete="off"
              />
            </label>
            <label class="campo stretto">
              <span>PIN</span>
              <input
                [value]="pin()"
                (input)="aggiornaPin($event)"
                maxlength="4"
                inputmode="numeric"
                placeholder="se serve"
                aria-label="PIN della stanza"
                autocomplete="off"
              />
            </label>
          </div>

          <div class="scelte">
            <button class="cta" type="submit" [disabled]="nome().trim().length < 2 || codice().length !== 4">
              Entra con il codice
            </button>
            <button
              class="ghost"
              type="button"
              (click)="crea()"
              [disabled]="nome().trim().length < 2 || (pin().length > 0 && pin().length !== 4)"
            >
              Crea una stanza
            </button>
          </div>
        </form>

        <p class="nota">Fiches virtuali. Nessun denaro reale.</p>
      </div>

      <div class="vetrina" aria-hidden="true">
        <img class="carta vetrina-carta c1" src="/assets/carte/denari-07.svg" alt="" />
        <img class="carta vetrina-carta c2" src="/assets/carte/coppe-asso.svg" alt="" />
        <img class="carta vetrina-carta c3" src="/assets/carte/spade-re.svg" alt="" />
        <div class="mucchio">
          <span class="ficha oro"></span>
          <span class="ficha verde"></span>
          <span class="ficha blu"></span>
        </div>
      </div>
    </section>
  `,
  styles: [
    `
      .home {
        display: grid;
        grid-template-columns: minmax(0, 1.05fr) minmax(0, 0.95fr);
        gap: clamp(1.5rem, 5vw, 4rem);
        align-items: center;
        padding: clamp(1rem, 4vw, 3rem) 0;
      }

      .intro {
        display: grid;
        gap: 1.1rem;
        max-width: 34rem;
      }

      .dati-partita {
        display: flex;
        flex-wrap: wrap;
        gap: 1.6rem;
        margin: 0;
        padding: 0;
        list-style: none;
      }

      .dati-partita li {
        display: grid;
        gap: 0.15rem;
      }

      .dati-partita strong {
        font-size: 1.45rem;
        font-weight: 700;
        color: var(--oro-chiaro);
        font-variant-numeric: tabular-nums;
      }

      .dati-partita span {
        font-size: 0.66rem;
        letter-spacing: 0.2em;
        text-transform: uppercase;
        color: rgba(246, 239, 223, 0.5);
      }

      .accesso {
        display: grid;
        gap: 0.75rem;
        margin-top: 0.4rem;
      }

      .codici {
        display: flex;
        gap: 0.75rem;
      }

      .stretto {
        flex: 0 1 8rem;
      }

      .scelte {
        display: flex;
        flex-wrap: wrap;
        gap: 0.6rem;
      }

      .campo {
        display: grid;
        gap: 0.4rem;
        flex: 1 1 14rem;
      }

      .campo span {
        font-size: 0.68rem;
        letter-spacing: 0.24em;
        text-transform: uppercase;
        color: rgba(246, 239, 223, 0.5);
      }

      .campo input {
        width: 100%;
        padding: 0.85rem 1rem;
        border-radius: 13px;
        border: 1px solid var(--bordo);
        background: rgba(3, 20, 14, 0.65);
        color: var(--crema);
        font-size: 0.95rem;
        transition: border-color 0.18s ease, box-shadow 0.18s ease;
      }

      .campo input:focus {
        outline: none;
        border-color: rgba(216, 169, 74, 0.7);
        box-shadow: 0 0 0 3px rgba(216, 169, 74, 0.16);
      }

      .campo input::placeholder {
        color: rgba(246, 239, 223, 0.32);
      }

      .vetrina {
        position: relative;
        display: grid;
        place-items: center;
        min-height: 300px;
      }

      .vetrina-carta {
        position: absolute;
        width: clamp(110px, 15vw, 156px);
        border-radius: 10px;
        box-shadow: 0 26px 50px rgba(0, 0, 0, 0.55);
        animation-delay: 60ms;
      }

      .c1 {
        transform: rotate(-14deg) translate(-64px, 12px);
      }

      .c2 {
        transform: rotate(-2deg) translateY(-8px);
        z-index: 2;
      }

      .c3 {
        transform: rotate(12deg) translate(62px, 16px);
      }

      .mucchio {
        position: absolute;
        bottom: 4%;
        display: flex;
      }

      .mucchio .ficha {
        width: 44px;
        height: 44px;
        margin-left: -12px;
        animation: cade 0.6s var(--molla) both;
      }

      .mucchio .ficha:nth-child(1) {
        animation-delay: 260ms;
      }

      .mucchio .ficha:nth-child(2) {
        animation-delay: 340ms;
      }

      .mucchio .ficha:nth-child(3) {
        animation-delay: 420ms;
      }

      @keyframes cade {
        from {
          opacity: 0;
          transform: translateY(-30px) scale(0.85);
        }
        to {
          opacity: 1;
          transform: none;
        }
      }

      @media (max-width: 860px) {
        .home {
          grid-template-columns: 1fr;
          gap: 1.5rem;
        }

        .vetrina {
          order: -1;
          min-height: 210px;
        }

        .vetrina-carta {
          width: 96px;
        }
      }
    `,
  ],
})
export class Home {
  readonly entra = output<RichiestaAccesso>();

  readonly nomeIniziale = input<string>('');

  readonly codiceIniziale = input<string>('');

  protected readonly nome = linkedSignal(() => this.nomeIniziale());

  protected readonly codice = linkedSignal(() => this.codiceIniziale().toUpperCase());

  protected readonly pin = signal('');

  protected aggiorna(evento: Event): void {
    this.nome.set((evento.target as HTMLInputElement).value);
  }

  protected aggiornaCodice(evento: Event): void {
    const valore = (evento.target as HTMLInputElement).value.toUpperCase().replace(/[^A-Z]/g, '');
    this.codice.set(valore.slice(0, 4));
  }

  protected aggiornaPin(evento: Event): void {
    this.pin.set((evento.target as HTMLInputElement).value.replace(/\D/g, '').slice(0, 4));
  }

  protected invia(evento: Event): void {
    evento.preventDefault();
    const nome = this.nome().trim();
    if (nome.length >= 2 && this.codice().length === 4) {
      this.entra.emit({ nome, codice: this.codice(), pin: this.pin() });
    }
  }

  protected crea(): void {
    const nome = this.nome().trim();
    if (nome.length >= 2) {
      this.entra.emit({ nome, codice: '', pin: this.pin() });
    }
  }
}
