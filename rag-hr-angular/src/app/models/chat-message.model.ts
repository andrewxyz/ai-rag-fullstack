import { SourceResponse } from './source-response.model';

export type MessageRole = 'user' | 'bot';

export interface ChatMessage {
  id: string;
  role: MessageRole;
  text: string;
  sources?: SourceResponse[];
  loading?: boolean;
}
