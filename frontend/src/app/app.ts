import { Component, computed, signal } from '@angular/core';

export type Carta = {
  seme: string;
  valore: string;
  file: string;
  punti: number;
};

export type GiocatoreTavolo = {
  indice: number;
  nome: string;
  fiches: number;
  debito: number;
  puntata: number;
  inGioco: boolean;
  diTurno: boolean;
  bot: boolean;
  numeroCarte: number;
  carte: Carta[];
};

export type ManoTavolo = {
  numero: number;
  maniTotali: number;
  cartaro: string;
  piatto: number;
  puntataMassima: number;
  chiusa: boolean;
  senzaPuntate: boolean;
  vincitori: string[];
  carteVincenti: Carta[];
};

export type ClassificaTavolo = {
  posizione: number;
  nome: string;
  punteggio: number;
  fiches: number;
};

export type StatoTavolo = {
  tuoIndice: number;
  nomeTuo: string;
  giocatori: GiocatoreTavolo[];
  mano: ManoTavolo;
  toccaATe: boolean;
  finita: boolean;
  eventi: string[];
  classifica: ClassificaTavolo[];
  puntataMinima: number;
  puntataMassima: number;
};

@Component({
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {
  protected readonly titolo = 'Stoppa';
  protected readonly stato = signal<StatoTavolo | null>(null);
  protected readonly errore = signal<string | null>(null);
  protected readonly puntata = signal(1);
  protected readonly attesa = signal(false);

  protected readonly io = computed(() => {
    const stato = this.stato();
    return stato ? stato.giocatori[stato.tuoIndice] : null;
  });

  protected readonly avversari = computed(() => {
    const stato = this.stato();
    return stato ? stato.giocatori.filter((giocatore) => giocatore.indice !== stato.tuoIndice) : [];
  });

  protected readonly costoVedo = computed(() => {
    const stato = this.stato();
    const io = this.io();
    return stato && io ? Math.max(0, stato.mano.puntataMassima - io.puntata) : 0;
  });

  protected readonly opzioniPuntata = computed(() => {
    const stato = this.stato();
    if (!stato) {
      return [];
    }
    const valori: number[] = [];
    for (let gettoni = stato.puntataMinima; gettoni <= stato.puntataMassima; gettoni++) {
      valori.push(gettoni);
    }
    return valori;
  });

  protected readonly puoPuntare = computed(() => this.opzioniPuntata().length > 0);

  protected readonly turnoNome = computed(
    () => this.stato()?.giocatori.find((giocatore) => giocatore.diTurno)?.nome ?? '',
  );

  constructor() {
    void this.carica();
  }

  protected async carica(): Promise<void> {
    await this.chiama('/api/tavolo');
  }

  protected async nuovaPartita(): Promise<void> {
    await this.chiama('/api/tavolo/nuova', {});
  }

  protected async passa(): Promise<void> {
    await this.chiama('/api/tavolo/passa', {});
  }

  protected async vedo(): Promise<void> {
    await this.chiama('/api/tavolo/vedo', {});
  }

  protected async punta(): Promise<void> {
    await this.chiama('/api/tavolo/punta', { gettoni: this.puntata() });
  }

  protected async prossimaMano(): Promise<void> {
    await this.chiama('/api/tavolo/prossima-mano', {});
  }

  protected cambiaPuntata(evento: Event): void {
    this.puntata.set(Number((evento.target as HTMLSelectElement).value));
  }

  protected dorsi(quante: number): number[] {
    return Array.from({ length: quante }, (_, indice) => indice);
  }

  protected immagine(file: string): string {
    return `/assets/carte/${file}.svg`;
  }

  private async chiama(url: string, corpo?: unknown): Promise<void> {
    this.attesa.set(true);
    this.errore.set(null);
    try {
      const risposta = await fetch(
        url,
        corpo === undefined
          ? undefined
          : {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify(corpo),
            },
      );
      const dati = (await risposta.json()) as StatoTavolo & { errore?: string };
      if (!risposta.ok) {
        this.errore.set(dati.errore ?? `Errore ${risposta.status}`);
        return;
      }
      this.stato.set(dati);
      if (this.puntata() < dati.puntataMinima || this.puntata() > dati.puntataMassima) {
        this.puntata.set(dati.puntataMinima);
      }
    } catch {
      this.errore.set('Backend non raggiungibile: avvia Spring Boot sulla porta 8080.');
    } finally {
      this.attesa.set(false);
    }
  }
}
