package it.stoppa.backend.tavolo;

import java.util.List;

public record GiocatoreTavolo(int indice, String nome, int fiches, int debito, int puntata, boolean inGioco,
		boolean diTurno, boolean bot, int numeroCarte, List<CartaTavolo> carte, boolean fichesInVista) {
}
