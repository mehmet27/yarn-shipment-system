import { ASSISTANT_PATH } from '@iplik/api-client/assistant';
import { http, HttpResponse } from 'msw';

/** Yardım asistanı sahte cevabı: gerçek modele gitmez; yalnızca ekranı denemek içindir. */
export function createAssistantHandlers() {
  return [
    http.get(`${ASSISTANT_PATH}/status`, () => HttpResponse.json({ available: true })),
    http.post(`${ASSISTANT_PATH}/ask`, () =>
      HttpResponse.json({ answer: 'Bu bir deneme cevabıdır.' }),
    ),
  ];
}
