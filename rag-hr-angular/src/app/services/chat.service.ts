import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ChatApiResponse } from '../models/chat-response.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ChatService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  sendMessage(question: string): Observable<ChatApiResponse> {
    return this.http.post<ChatApiResponse>(`${this.apiUrl}/api/v1/rag/chat`, { question });
  }

  ingestDefault(): Observable<string> {
    return this.http.post(`${this.apiUrl}/api/documents/ingest`, null, { responseType: 'text' });
  }

  uploadDocument(file: File): Observable<string> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post(`${this.apiUrl}/api/documents/upload`, formData, { responseType: 'text' });
  }
}
