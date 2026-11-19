import { Component, signal } from '@angular/core';

type BackendStatus = 'checking' | 'up' | 'down';
type LobbyStatus = 'idle' | 'connecting' | 'open' | 'closed';

@Component({
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {
  private socket?: WebSocket;

  protected readonly title = signal('Stoppa');
  protected readonly backend = signal<BackendStatus>('checking');
  protected readonly lobby = signal<LobbyStatus>('idle');

  constructor() {
    void this.checkBackend();
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

  private async checkBackend(): Promise<void> {
    try {
      const response = await fetch('/api/health');
      this.backend.set(response.ok ? 'up' : 'down');
    } catch {
      this.backend.set('down');
    }
  }
}
