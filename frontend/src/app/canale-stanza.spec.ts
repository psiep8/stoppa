import { AvvisoCanale, CanaleStanza } from './canale-stanza';
import { StanzaLobby, StatoTavolo } from './gioco';

const CODICE = 'ABCD';
const SESSIONE = 'sessione-1';

/** Lo stesso ritardo con cui CanaleStanza riprova dopo una chiusura. */
const RITARDO_RICONNESSIONE = 1500;

const ENTRATA = { tipo: 'entra', codice: CODICE, sessione: SESSIONE };

/** Un WebSocket che non si collega a niente: il test decide quando apre e cosa arriva. */
class WebSocketFinto {
  static readonly OPEN = 1;

  static istanze: WebSocketFinto[] = [];

  readonly inviati: string[] = [];

  readyState = 0;

  onopen: (() => void) | null = null;

  onmessage: ((evento: { data: string }) => void) | null = null;

  onclose: (() => void) | null = null;

  onerror: (() => void) | null = null;

  constructor(readonly url: string) {
    WebSocketFinto.istanze.push(this);
  }

  send(dato: string): void {
    this.inviati.push(dato);
  }

  close(): void {
    this.readyState = 3;
    this.onclose?.();
  }

  apre(): void {
    this.readyState = WebSocketFinto.OPEN;
    this.onopen?.();
  }

  riceve(messaggio: unknown): void {
    this.onmessage?.({ data: JSON.stringify(messaggio) });
  }

  /** I messaggi partiti dal servizio, gia' decodificati. */
  partiti(): unknown[] {
    return this.inviati.map((invio) => JSON.parse(invio));
  }
}

const STANZA: StanzaLobby = {
  codice: CODICE,
  posti: [{ indice: 0, nome: 'Peppa', bot: false }],
  postiTotali: 8,
  minimoGiocatori: 4,
  avviata: false,
  chiusa: false,
  conPin: false,
  creatore: true,
  tuoIndice: 0,
};

const STATO = { tuoIndice: 0, eventi: ['Passi'] } as unknown as StatoTavolo;

function ultimoSocket(): WebSocketFinto {
  const socket = WebSocketFinto.istanze.at(-1);
  if (!socket) {
    throw new Error('nessun socket aperto');
  }
  return socket;
}

describe('CanaleStanza', () => {
  let canale: CanaleStanza;

  beforeEach(() => {
    vi.useFakeTimers();
    vi.stubGlobal('WebSocket', WebSocketFinto);
    WebSocketFinto.istanze = [];
    canale = new CanaleStanza();
  });

  afterEach(() => {
    canale.chiudi();
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  /** Apre il canale e fa arrivare l'open, come farebbe il server. */
  function apriCanale(): WebSocketFinto {
    canale.apri(CODICE, SESSIONE);
    const socket = ultimoSocket();
    socket.apre();
    return socket;
  }

  function raccogliAvvisi(): { avvisi: AvvisoCanale[]; stacca: () => void } {
    const avvisi: AvvisoCanale[] = [];
    return { avvisi, stacca: canale.ascolta((avviso) => avvisi.push(avviso)) };
  }

  it('legge stanza, stato ed errore dal socket', () => {
    const { avvisi } = raccogliAvvisi();
    const socket = apriCanale();

    expect(socket.partiti()).toEqual([ENTRATA]);

    socket.riceve({ tipo: 'stanza', stanza: STANZA });
    expect(canale.stanza()).toEqual(STANZA);

    socket.riceve({ tipo: 'stato', stato: STATO });
    expect(avvisi).toEqual([{ tipo: 'stato', stato: STATO }]);

    socket.riceve({ tipo: 'errore', messaggio: "non e' il tuo turno" });
    expect(avvisi.at(-1)).toEqual({ tipo: 'errore', messaggio: "non e' il tuo turno" });

    socket.riceve({ tipo: 'errore', messaggio: 'sessione non valida', motivo: 'ricomincia' });
    expect(avvisi.at(-1)).toEqual({ tipo: 'errore', messaggio: 'sessione non valida', motivo: 'ricomincia' });
  });

  it('rimanda entra quando si riapre la stessa stanza', () => {
    const socket = apriCanale();

    canale.apri(CODICE, SESSIONE);

    expect(WebSocketFinto.istanze).toHaveLength(1);
    expect(socket.partiti()).toEqual([ENTRATA, ENTRATA]);
  });

  it('smette di ascoltare quando si usa la funzione restituita da ascolta', () => {
    const { avvisi, stacca } = raccogliAvvisi();
    const socket = apriCanale();

    socket.riceve({ tipo: 'errore', messaggio: 'primo' });
    stacca();
    socket.riceve({ tipo: 'errore', messaggio: 'secondo' });

    expect(avvisi).toEqual([{ tipo: 'errore', messaggio: 'primo' }]);
  });

  it('dopo la chiusura si riconnette e rimanda entra con la stessa stanza e sessione', () => {
    const primo = apriCanale();

    primo.close();
    expect(WebSocketFinto.istanze).toHaveLength(1);

    vi.advanceTimersByTime(RITARDO_RICONNESSIONE);
    expect(WebSocketFinto.istanze).toHaveLength(2);

    const secondo = ultimoSocket();
    secondo.apre();
    expect(secondo.partiti()).toEqual([ENTRATA]);
  });
});
