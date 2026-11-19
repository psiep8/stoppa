import { Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { RouterOutlet } from '@angular/router';

type BackendStatus = 'checking' | 'up' | 'down';
type LobbyStatus = 'idle' | 'connecting' | 'open' | 'closed';

@Component({
  imports: [RouterOutlet],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {
  private readonly http = inject(HttpClient);
  private socket?: WebSocket;

  protected readonly title = signal('Stoppa');
  protected readonly backend = signal<BackendStatus>('checking');
  protected readonly lobby = signal<LobbyStatus>('idle');

  constructor() {
    this.http.get<{ status: string }>('/api/health').subscribe({
      next: () => this.backend.set('up'),
      error: () => this.backend.set('down'),
    });
  }

  protected connectLobby(): void {
    this.socket?.close();
    this.lobby.set('connecting');
    const protocol = location.protocol === 'https:' ? 'wss' : 'ws';
    const socket = new WebSocket(`${protocol}://${location.host}/ws/lobby`);
    this.socket = socket;
    socket.onopen = () => this.lobby.set('open');
    socket.onclose = () => this.lobby.set('closed');
    socket.onerror = () => this.lobby.set('closed');
  }
}
