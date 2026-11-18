import { httpClient } from './httpClient';

export const ASSISTANT_PATH = '/api/v1/assistant';

/** Sohbetteki önceki bir mesaj. */
export type AssistantMessage = { role: 'user' | 'assistant'; text: string };

/** Yardım asistanı kullanıma hazır mı: sunucuda anahtar tanımlı değilse hazır değildir ve balon gösterilmez. */
export async function getAssistantStatus(): Promise<{ available: boolean }> {
  const response = await httpClient.get<{ available: boolean }>(`${ASSISTANT_PATH}/status`);
  return response.data;
}

/** Soruyu asistana sorar; asistan yalnızca bilgi verir, hiçbir kaydı değiştirmez. */
export async function askAssistant(
  question: string,
  history: readonly AssistantMessage[],
): Promise<{ answer: string }> {
  const response = await httpClient.post<{ answer: string }>(`${ASSISTANT_PATH}/ask`, {
    question,
    history,
  });
  return response.data;
}
