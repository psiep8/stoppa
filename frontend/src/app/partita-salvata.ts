export type SchermataSalvata = 'lobby' | 'tavolo';

export type PartitaSalvata = { codice: string; sessione: string; schermata: SchermataSalvata };

const CHIAVE = 'stoppa.partita';

/**
 * La partita in corso di questa scheda. sessionStorage e' per scheda: due
 * finestre aperte sullo stesso browser restano due giocatori diversi, e
 * ricaricare la pagina non fa perdere stanza e sessione.
 */
export function leggiPartita(): PartitaSalvata | null {
  try {
    const salvata = sessionStorage.getItem(CHIAVE);
    if (!salvata) {
      return null;
    }
    const partita = JSON.parse(salvata) as Partial<PartitaSalvata>;
    if (!partita.codice || !partita.sessione) {
      return null;
    }
    return {
      codice: partita.codice,
      sessione: partita.sessione,
      schermata: partita.schermata === 'tavolo' ? 'tavolo' : 'lobby',
    };
  } catch {
    return null;
  }
}

export function salvaPartita(partita: PartitaSalvata): void {
  try {
    sessionStorage.setItem(CHIAVE, JSON.stringify(partita));
  } catch {
    // storage vietato: si gioca lo stesso, ricaricare riporta alla home
  }
}

export function cancellaPartita(): void {
  try {
    sessionStorage.removeItem(CHIAVE);
  } catch {
    // niente da fare
  }
}
