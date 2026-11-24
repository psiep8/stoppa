import { Injectable, signal } from '@angular/core';

/**
 * La vista del tavolo. La POV nasce per il telefono, ma si accende anche da PC
 * con il pulsante accanto al microfono: lo stato vive qui perche' quel pulsante
 * sta nella riga della voce, fuori dal tavolo.
 */
@Injectable({ providedIn: 'root' })
export class Vista {
  /** Si parte dalla POV anche da PC: il tavolo vero si apre dal pulsante. */
  readonly pov = signal(true);

  /** Vero solo mentre il tavolo e' a schermo: fuori non serve il pulsante. */
  readonly inGioco = signal(false);

  /**
   * POV immersiva: tavolo in prospettiva e sfocato quando le carte sono alzate,
   * swipe verticale per alternare carte e tavolo. Spenta di default: la POV
   * normale tiene tavolo, mano e comandi visibili insieme.
   */
  readonly immersiva = signal(false);

  /** Carte alzate (tavolo sfocato) oppure abbassate (tavolo chiaro e comandi). */
  readonly alzate = signal(true);

  alterna(): void {
    this.pov.update((pov) => !pov);
  }

  alternaImmersiva(): void {
    this.immersiva.update((attiva) => !attiva);
    this.alzate.set(true);
  }
}

