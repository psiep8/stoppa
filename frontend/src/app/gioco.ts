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
  /** Falso quando il giocatore tiene le fiches coperte: fiches vale -1. */
  fichesInVista: boolean;
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
  /** Chi apre le dichiarazioni a fine mano (ultimo rilancio o primo puntatore). */
  dichiarante: string;
  puntiVincenti: number;
  /** Fase di dichiarazione aperta: le carte sono ancora coperte. */
  inDichiarazione: boolean;
  /** Punti dichiarati da chi ha aperto (0 se non ha ancora parlato). */
  dichiarazione: number;
  /** Esito deciso dal server: accettata, buono, buono-di-mano, non-sei-buono. */
  esitoDichiarazione: string;
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
  puoiDichiarare: boolean;
  puoiRispondere: boolean;
};

export type PostoLobby = { indice: number; nome: string; bot: boolean };

export type StanzaLobby = {
  codice: string;
  posti: PostoLobby[];
  postiTotali: number;
  minimoGiocatori: number;
  avviata: boolean;
  chiusa: boolean;
  conPin: boolean;
  creatore: boolean;
  tuoIndice: number;
};

export type IngressoLobby = { sessione: string; stanza: StanzaLobby };

/** Quello che sceglie la home: nome, codice stanza (vuoto = crea) e PIN. */
export type RichiestaAccesso = { nome: string; codice: string; pin: string };

export type ErroreApi = { errore?: string };

export async function inviaApi<T>(
  url: string,
  metodo: 'GET' | 'POST' | 'DELETE',
  corpo?: unknown,
  sessione?: string,
): Promise<T> {
  const intestazioni: Record<string, string> = {};
  if (corpo !== undefined) {
    intestazioni['Content-Type'] = 'application/json';
  }
  if (sessione) {
    intestazioni['X-Sessione'] = sessione;
  }
  const risposta = await fetch(url, {
    method: metodo,
    headers: Object.keys(intestazioni).length ? intestazioni : undefined,
    body: corpo === undefined ? undefined : JSON.stringify(corpo),
  });
  const dati = (await risposta.json()) as T & ErroreApi;
  if (!risposta.ok) {
    throw new Error(dati.errore ?? `Errore ${risposta.status}`);
  }
  return dati;
}

export function chiamataApi<T>(url: string, corpo?: unknown, sessione?: string): Promise<T> {
  return inviaApi<T>(url, corpo === undefined ? 'GET' : 'POST', corpo, sessione);
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
