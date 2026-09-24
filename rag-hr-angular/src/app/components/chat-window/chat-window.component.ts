import { Component, signal, inject, ElementRef, ViewChild, afterNextRender } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatService } from '../../services/chat.service';
import { ChatMessage } from '../../models/chat-message.model';
import { MessageBubbleComponent } from '../message-bubble/message-bubble.component';

@Component({
  selector: 'app-chat-window',
  standalone: true,
  imports: [CommonModule, FormsModule, MessageBubbleComponent],
  templateUrl: './chat-window.component.html'
})
export class ChatWindowComponent {
  @ViewChild('messageList') private messageList!: ElementRef<HTMLDivElement>;

  private chatService = inject(ChatService);

  messages = signal<ChatMessage[]>([
    {
      id: 'welcome',
      role: 'bot',
      text: "Hello! I'm your HR Policy Assistant. Ask me anything about company policies — leave, benefits, or attendance."
    }
  ]);
  isLoading = signal(false);
  inputText = '';

  constructor() {
    afterNextRender(() => this.scrollToBottom());
  }

  sendMessage() {
    const question = this.inputText.trim();
    if (!question || this.isLoading()) return;

    this.inputText = '';

    this.messages.update(msgs => [...msgs, {
      id: crypto.randomUUID(),
      role: 'user',
      text: question
    }]);

    const loadingId = crypto.randomUUID();
    this.messages.update(msgs => [...msgs, {
      id: loadingId,
      role: 'bot',
      text: '',
      loading: true
    }]);

    this.isLoading.set(true);
    this.scrollToBottom();

    this.chatService.sendMessage(question).subscribe({
      next: (res) => {
        this.messages.update(msgs =>
          msgs.map(m => m.id === loadingId
            ? { id: loadingId, role: 'bot', text: res.answer, sources: res.sources }
            : m
          )
        );
        this.isLoading.set(false);
        this.scrollToBottom();
      },
      error: () => {
        this.messages.update(msgs =>
          msgs.map(m => m.id === loadingId
            ? { id: loadingId, role: 'bot', text: 'Sorry, I encountered an error. Please check the backend is running.' }
            : m
          )
        );
        this.isLoading.set(false);
        this.scrollToBottom();
      }
    });
  }

  onKeyDown(event: KeyboardEvent) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  private scrollToBottom() {
    setTimeout(() => {
      if (this.messageList?.nativeElement) {
        this.messageList.nativeElement.scrollTop = this.messageList.nativeElement.scrollHeight;
      }
    }, 0);
  }
}
