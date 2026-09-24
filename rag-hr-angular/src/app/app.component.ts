import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ChatWindowComponent } from './components/chat-window/chat-window.component';
import { KnowledgeSidebarComponent } from './components/knowledge-sidebar/knowledge-sidebar.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, ChatWindowComponent, KnowledgeSidebarComponent],
  templateUrl: './app.component.html'
})
export class AppComponent {
  sidebarOpen = signal(false);
  darkMode = signal(false);

  toggleDark() {
    this.darkMode.update(v => !v);
    document.documentElement.classList.toggle('dark');
  }

  openSidebar() {
    this.sidebarOpen.set(true);
  }

  closeSidebar() {
    this.sidebarOpen.set(false);
  }
}
