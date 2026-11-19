import { Component, inject, signal } from '@angular/core';
import { Home } from './home';
import { Lobby } from './lobby';
import { Suoni } from './suoni';
import { Tavolo } from './tavolo';

type Schermata = 'home' | 'lobby' | 'tavolo';

@Component({
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
  imports: [Home, Lobby, Tavolo],
})
export class App {
  private readonly suoni = inject(Suoni);

  protected readonly schermata = signal<Schermata>('home');
  protected readonly nome = signal(nomeSalvato());
  protected readonly stanza = signal('');
  protected readonly audio = signal(true);

  constructor() {
    document.addEventListener('pointerdown', () => this.suoni.sblocca(), { once: true });
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

  protected avviaPartita(codice: string): void {
    this.stanza.set(codice);
    this.schermata.set('tavolo');
  }

  protected tornaHome(): void {
    this.schermata.set('home');
  }

  protected tornaAllaLobby(): void {
    this.schermata.set('lobby');
  }

  protected alternaAudio(): void {
    const attivo = !this.audio();
    this.audio.set(attivo);
    this.suoni.impostaAttivo(attivo);
  }
}

function nomeSalvato(): string {
  try {
    return localStorage.getItem('stoppa.nome') ?? '';
  } catch {
    return '';
  }
}
