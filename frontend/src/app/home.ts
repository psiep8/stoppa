import { Component, input, linkedSignal, output, signal } from '@angular/core';
import { RichiestaAccesso } from './gioco';

@Component({
  selector: 'app-home',
  template: `
    <section class="home">
      <div class="intro">
        <h2 class="titolo">
          Quaranta carte, un tavolo, da quattro a otto persone.
        </h2>

        <p class="sommario">
          Fiches virtuali, nessun denaro. Si entra con un codice o con il link di chi ha
          aperto la stanza.
        </p>

        <form class="accesso" (submit)="invia($event)">
          <label class="campo">
            <span>Nome</span>
            <input
              [value]="nome()"
              (input)="aggiorna($event)"
              maxlength="14"
              placeholder="Come ti chiami"
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

        <dl class="dati-partita">
          <div><dt>Giocatori</dt><dd>4–8</dd></div>
          <div><dt>Carte a testa</dt><dd>3</dd></div>
          <div><dt>Puntata</dt><dd>1–10 gettoni</dd></div>
        </dl>
      </div>

      <figure class="vetrina">
        <div class="tavola" aria-hidden="true">
          <img class="carta vetrina-carta c1" src="/assets/carte/denari-07.svg" alt="" />
          <img class="carta vetrina-carta c2" src="/assets/carte/coppe-asso.svg" alt="" />
          <img class="carta vetrina-carta c3" src="/assets/carte/spade-re.svg" alt="" />
          <div class="mucchio">
            <span class="ficha oro"></span>
            <span class="ficha verde"></span>
            <span class="ficha blu"></span>
          </div>
        </div>
        <figcaption>Carte napoletane · quaranta · CC0</figcaption>
      </figure>
    </section>
  `,
  styles: [
    `
      .home {
        display: grid;
        grid-template-columns: minmax(0, 1.05fr) minmax(0, 0.95fr);
        gap: clamp(2rem, 6vw, 4.5rem);
        align-items: start;
        padding: clamp(0.5rem, 2vw, 1.5rem) 0;
      }

      .intro {
        display: grid;
        gap: 1.25rem;
        max-width: 36rem;
      }

      .titolo {
        margin: 0;
        font-family: var(--serif);
        font-size: clamp(2rem, 4.2vw, 3.05rem);
        font-weight: 600;
        line-height: 1.02;
        letter-spacing: -0.02em;
        text-wrap: balance;
        color: var(--inchiostro);
      }

      .sommario {
        margin: 0;
        max-width: 48ch;
        font-size: 1.02rem;
        line-height: 1.6;
        color: var(--inchiostro-tenue);
      }

      .accesso {
        display: grid;
        gap: 1.1rem;
        padding-top: 0.6rem;
        border-top: 1px solid var(--bordo);
        max-width: 30rem;
      }

      .codici {
        display: flex;
        gap: 1.4rem;
      }

      .campo {
        display: grid;
        gap: 0.3rem;
      }

      .campo span {
        font-size: 0.68rem;
        font-weight: 600;
        letter-spacing: 0.16em;
        text-transform: uppercase;
        color: var(--testo-tenue);
      }

      /* Campi come righe di un modulo cartaceo: solo la linea di base. */
      .campo input {
        width: 100%;
        padding: 0.4rem 0;
        border: 0;
        border-bottom: 1px solid var(--bordo);
        border-radius: 0;
        background: transparent;
        color: var(--inchiostro);
        font-family: var(--serif);
        font-size: 1.18rem;
      }

      .campo input:focus {
        outline: none;
        border-bottom-color: var(--oro);
        box-shadow: 0 2px 0 -1px var(--oro);
      }

      .campo input::placeholder {
        color: rgba(242, 232, 213, 0.34);
        font-style: italic;
      }

      .stretto {
        flex: 0 1 8.5rem;
      }

      .stretto input {
        font-variant-numeric: tabular-nums;
        letter-spacing: 0.18em;
        text-transform: uppercase;
      }

      .scelte {
        display: flex;
        flex-wrap: wrap;
        gap: 0.6rem;
        padding-top: 0.3rem;
      }

      .dati-partita {
        display: flex;
        flex-wrap: wrap;
        gap: 0 1.6rem;
        margin: 0.4rem 0 0;
        padding-top: 0.7rem;
        border-top: 1px solid var(--bordo-tenue);
      }

      .dati-partita div {
        display: flex;
        align-items: baseline;
        gap: 0.45rem;
      }

      .dati-partita dt {
        font-size: 0.68rem;
        letter-spacing: 0.12em;
        text-transform: uppercase;
        color: var(--testo-tenue);
      }

      .dati-partita dd {
        margin: 0;
        font-family: var(--serif);
        font-size: 0.98rem;
        font-variant-numeric: tabular-nums;
      }

      /* La vetrina è una tavola: feltro pieno, nessun riflesso. */
      .vetrina {
        margin: 0;
        display: grid;
        gap: 0.6rem;
      }

      .tavola {
        position: relative;
        display: grid;
        place-items: center;
        min-height: clamp(240px, 30vw, 340px);
        border-radius: 3px;
        background-color: var(--feltro);
        background-image: var(--panno);
        border: 1px solid var(--feltro-nero);
        overflow: hidden;
      }

      .vetrina-carta {
        position: absolute;
        width: clamp(104px, 14vw, 148px);
        animation-delay: 60ms;
      }

      /* rotate/translate separati: così l'animazione di ingresso non
         cancella la ventagliata quando finisce. */
      .c1 {
        rotate: -11deg;
        translate: -58px 14px;
      }

      .c2 {
        rotate: -1deg;
        translate: 0 -6px;
        z-index: 2;
      }

      .c3 {
        rotate: 10deg;
        translate: 56px 18px;
      }

      .mucchio {
        position: absolute;
        bottom: 6%;
        display: flex;
      }

      .mucchio .ficha {
        width: 38px;
        height: 38px;
        margin-left: -10px;
        animation: cade 0.55s var(--molla) both;
      }

      .mucchio .ficha:nth-child(1) {
        animation-delay: 220ms;
      }

      .mucchio .ficha:nth-child(2) {
        animation-delay: 300ms;
      }

      .mucchio .ficha:nth-child(3) {
        animation-delay: 380ms;
      }

      @keyframes cade {
        from {
          opacity: 0;
          transform: translateY(-24px);
        }
        to {
          opacity: 1;
          transform: none;
        }
      }

      figcaption {
        font-size: 0.7rem;
        letter-spacing: 0.14em;
        text-transform: uppercase;
        color: var(--testo-tenue);
      }

      @media (max-width: 860px) {
        .home {
          grid-template-columns: 1fr;
          gap: 1.8rem;
        }

        .vetrina {
          order: -1;
        }

        .tavola {
          min-height: 200px;
        }

        .vetrina-carta {
          width: 92px;
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
