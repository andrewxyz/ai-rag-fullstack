import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ChatMessage } from '../../models/chat-message.model';

@Component({
  selector: 'app-message-bubble',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './message-bubble.component.html'
})
export class MessageBubbleComponent {
  message = input.required<ChatMessage>();
  sourcesExpanded = false;

  toggleSources() {
    this.sourcesExpanded = !this.sourcesExpanded;
  }
}
