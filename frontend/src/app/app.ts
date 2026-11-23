import { Component, inject, signal } from '@angular/core';
import { CanaleStanza } from './canale-stanza';
import { Home } from './home';
import { Lobby } from './lobby';
import { PartitaSalvata, cancellaPartita, leggiPartita, salvaPartita } from './partita-salvata';
import { Suoni } from './suoni';
import { Tavolo } from './tavolo';

type Schermata = 'home' | PartitaSalvata['schermata'];

@Component({
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
  imports: [Home, Lobby, Tavolo],
})
export class App {
  private readonly suoni = inject(Suoni);

  private readonly canale = inject(CanaleStanza);

  protected readonly schermata = signal<Schermata>('home');
  protected readonly nome = signal(nomeSalvato());
  protected readonly stanza = signal('');
  protected readonly sessione = signal('');
  protected readonly audio = signal(true);

  constructor() {
    document.addEventListener('pointerdown', () => this.suoni.sblocca(), { once: true });
    // lo stato salvato non vale piu': si riparte dalla home, che lo cancella
    this.canale.ascolta((avviso) => {
      if (avviso.tipo === 'errore' && avviso.motivo === 'ricomincia') {
        this.tornaHome();
      }
    });
    const salvata = leggiPartita();
    if (salvata) {
      this.stanza.set(salvata.codice);
      this.sessione.set(salvata.sessione);
      this.schermata.set(salvata.schermata);
    }
  }

  protected vaiAllaLobby(nome: string): void {
    this.nome.set(nome);
    try {
      localStorage.setItem('stoppa.nome', nome);
    } catch {
      // localStorage puo' essere vietato: si gioca lo stesso
    }
    this.schermata.set('lobby');
  }

  /** La lobby ha una stanza e una sessione: da qui si possono riprendere al reload. */
  protected inLobby(partita: { codice: string; sessione: string }): void {
    this.ricorda(partita, 'lobby');
  }

  protected avviaPartita(partita: { codice: string; sessione: string }): void {
    this.ricorda(partita, 'tavolo');
    this.schermata.set('tavolo');
  }

  protected tornaHome(): void {
    this.canale.chiudi();
    cancellaPartita();
    this.stanza.set('');
    this.sessione.set('');
    this.schermata.set('home');
  }

  protected tornaAllaLobby(): void {
    this.schermata.set('lobby');
    this.ricorda({ codice: this.stanza(), sessione: this.sessione() }, 'lobby');
  }

  protected alternaAudio(): void {
    const attivo = !this.audio();
    this.audio.set(attivo);
    this.suoni.impostaAttivo(attivo);
  }

  private ricorda(partita: { codice: string; sessione: string }, schermata: PartitaSalvata['schermata']): void {
    this.stanza.set(partita.codice);
    this.sessione.set(partita.sessione);
    if (partita.codice && partita.sessione) {
      salvaPartita({ codice: partita.codice, sessione: partita.sessione, schermata });
    }
  }
}

function nomeSalvato(): string {
  try {
    return localStorage.getItem('stoppa.nome') ?? '';
  } catch {
    return '';
  }
}
