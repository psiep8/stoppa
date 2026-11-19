import { Component, input, linkedSignal, output } from '@angular/core';

@Component({
  selector: 'app-home',
  template: `
    <section class="home">
      <div class="intro">
        <p class="occhiello">Tavolo di carte · 4–8 giocatori</p>
        <h2 class="titolo">
          Tre carte.<br />
          Un piatto.<br />
          <em>Nessuna certezza.</em>
        </h2>
        <p class="testo">
          La stoppa si gioca di bluff: punti, vedi, rilanci. Chi resta con il punto più alto porta a casa le fiches.
        </p>

        <form class="accesso" (submit)="invia($event)">
          <label class="campo">
            <span>Come ti chiami</span>
            <input
              [value]="nome()"
              (input)="aggiorna($event)"
              maxlength="14"
              placeholder="Il tuo nome al tavolo"
              autocomplete="off"
            />
          </label>
          <button class="cta" type="submit" [disabled]="nome().trim().length < 2">Siediti al tavolo</button>
        </form>

        <p class="nota">Solo fiches virtuali. Nessuna vincita in denaro, nessun acquisto.</p>
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

      .titolo {
        margin: 0;
        font-size: clamp(2.1rem, 6vw, 3.6rem);
        line-height: 1.02;
        letter-spacing: -0.02em;
        font-weight: 700;
      }

      .titolo em {
        font-family: Georgia, 'Times New Roman', serif;
        font-style: italic;
        font-weight: 400;
        color: var(--oro-chiaro);
      }

      .accesso {
        display: flex;
        flex-wrap: wrap;
        align-items: flex-end;
        gap: 0.75rem;
        margin-top: 0.4rem;
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
  readonly entra = output<string>();

  readonly nomeIniziale = input<string>('');

  protected readonly nome = linkedSignal(() => this.nomeIniziale());

  protected aggiorna(evento: Event): void {
    this.nome.set((evento.target as HTMLInputElement).value);
  }

  protected invia(evento: Event): void {
    evento.preventDefault();
    const nome = this.nome().trim();
    if (nome.length >= 2) {
      this.entra.emit(nome);
    }
  }
}
