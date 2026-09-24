import { SourceResponse } from './source-response.model';

export interface ChatApiResponse {
  answer: string;
  sources: SourceResponse[];
}
