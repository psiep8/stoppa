/** Un colore di ficha per giocatore: fino a otto posti, nessuno ripete. */
export const COLORI = ['oro', 'rosso', 'verde', 'blu', 'nero', 'bianco', 'viola', 'arancio'];

export function coloreDi(indice: number): string {
  return COLORI[Math.abs(indice) % COLORI.length];
}
