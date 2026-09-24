import { Component, output, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ChatService } from '../../services/chat.service';

type IngestStatus = 'idle' | 'loading' | 'success' | 'error';

@Component({
  selector: 'app-knowledge-sidebar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './knowledge-sidebar.component.html'
})
export class KnowledgeSidebarComponent {
  close = output<void>();

  private chatService = inject(ChatService);

  ingestStatus = signal<IngestStatus>('idle');
  ingestMessage = signal('');
  uploadStatus = signal<IngestStatus>('idle');
  uploadMessage = signal('');
  dragOver = signal(false);

  ingestDefault() {
    this.ingestStatus.set('loading');
    this.chatService.ingestDefault().subscribe({
      next: (msg) => {
        this.ingestStatus.set('success');
        this.ingestMessage.set(msg);
      },
      error: () => {
        this.ingestStatus.set('error');
        this.ingestMessage.set('Ingestion failed. Is the backend running?');
      }
    });
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files?.[0]) {
      this.uploadFile(input.files[0]);
      input.value = '';
    }
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    this.dragOver.set(false);
    const file = event.dataTransfer?.files[0];
    if (file) this.uploadFile(file);
  }

  onDragOver(event: DragEvent) {
    event.preventDefault();
    this.dragOver.set(true);
  }

  onDragLeave() {
    this.dragOver.set(false);
  }

  private uploadFile(file: File) {
    if (!file.name.endsWith('.pdf')) {
      this.uploadStatus.set('error');
      this.uploadMessage.set('Only PDF files are supported.');
      return;
    }
    this.uploadStatus.set('loading');
    this.uploadMessage.set(`Uploading ${file.name}…`);
    this.chatService.uploadDocument(file).subscribe({
      next: (msg) => {
        this.uploadStatus.set('success');
        this.uploadMessage.set(msg);
      },
      error: () => {
        this.uploadStatus.set('error');
        this.uploadMessage.set('Upload failed. Is the backend running?');
      }
    });
  }
}
