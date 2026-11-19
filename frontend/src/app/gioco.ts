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

export type ErroreApi = { errore?: string };

export async function chiamataApi<T>(url: string, corpo?: unknown): Promise<T> {
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
  const dati = (await risposta.json()) as T & ErroreApi;
  if (!risposta.ok) {
    throw new Error(dati.errore ?? `Errore ${risposta.status}`);
  }
  return dati;
}

export function etichettaSeme(seme: string): string {
  return seme.charAt(0) + seme.slice(1).toLowerCase();
}

export function etichettaValore(valore: string): string {
  const nomi: Record<string, string> = {
    ASSO: 'Asso',
    DUE: 'Due',
    TRE: 'Tre',
    QUATTRO: 'Quattro',
    CINQUE: 'Cinque',
    SEI: 'Sei',
    SETTE: 'Sette',
    FANTE: 'Fante',
    CAVALLO: 'Cavallo',
    RE: 'Re',
  };
  return nomi[valore] ?? valore;
}
