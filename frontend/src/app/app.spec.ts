import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { App } from './app';
import { AvvisoCanale, CanaleStanza } from './canale-stanza';
import { StanzaLobby } from './gioco';

/** Il canale senza WebSocket: il test decide quando arriva un avviso. */
class CanaleFinto {
  readonly stanza = signal<StanzaLobby | null>(null);

  private lettori = new Set<(avviso: AvvisoCanale) => void>();

  ascolta(lettore: (avviso: AvvisoCanale) => void): () => void {
    this.lettori.add(lettore);
    return () => this.lettori.delete(lettore);
  }

  avvisa(avviso: AvvisoCanale): void {
    this.lettori.forEach((lettore) => lettore(avviso));
  }

  apri(): void {}

  chiudi(): void {}

  assorbi(): void {}

  invia(): boolean {
    return false;
  }
}

interface Aperta {
  fixture: ComponentFixture<App>;
  pagina: HTMLElement;
  canale: CanaleFinto;
}

/** Monta l'app come farebbe un reload, con o senza una partita gia' salvata. */
function apri(schermata?: 'lobby' | 'tavolo'): Aperta {
  if (schermata) {
    sessionStorage.setItem(
      'stoppa.partita',
      JSON.stringify({ codice: 'ABCD', sessione: 'sessione-1', schermata }),
    );
  }
  vi.stubGlobal('fetch', vi.fn(() => Promise.reject(new Error('offline'))));
  const canale = new CanaleFinto();
  TestBed.configureTestingModule({ providers: [{ provide: CanaleStanza, useValue: canale }] });
  const fixture = TestBed.createComponent(App);
  fixture.detectChanges();
  return { fixture, pagina: fixture.nativeElement as HTMLElement, canale };
}

function pulisciStorage(): void {
  sessionStorage.clear();
  localStorage.clear();
}

describe('App', () => {
  // ogni test parte da un browser senza stato: niente partita e niente nome salvati
  beforeEach(pulisciStorage);

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('senza partita salvata apre la home', () => {
    const { pagina } = apri();

    expect(pagina.querySelector('h1')?.textContent).toContain('Stoppa');
    expect(pagina.querySelector('section.home')).toBeTruthy();
    expect(pagina.querySelector('section.lobby')).toBeNull();
  });

  it('con una partita salvata in lobby riapre la lobby, non la home', () => {
    const { pagina } = apri('lobby');

    expect(pagina.querySelector('section.lobby')).toBeTruthy();
    expect(pagina.querySelector('section.home')).toBeNull();
    expect(sessionStorage.getItem('stoppa.partita')).not.toBeNull();
  });

  it('con una partita salvata al tavolo riapre il tavolo, non la home', () => {
    const { pagina } = apri('tavolo');

    expect(pagina.querySelector('section.scena')).toBeTruthy();
    expect(pagina.querySelector('section.home')).toBeNull();
  });

  it('con motivo ricomincia torna alla home e cancella la partita salvata', () => {
    const { fixture, pagina, canale } = apri('lobby');
    expect(pagina.querySelector('section.lobby')).toBeTruthy();

    canale.avvisa({ tipo: 'errore', messaggio: 'sessione non valida: ricomincia dalla home', motivo: 'ricomincia' });
    fixture.detectChanges();

    expect(pagina.querySelector('section.home')).toBeTruthy();
    expect(sessionStorage.getItem('stoppa.partita')).toBeNull();
  });
});
