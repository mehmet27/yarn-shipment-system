import type { AssistantMessage } from '@iplik/api-client/assistant';
import { askAssistant } from '@iplik/api-client/assistant';
import { toProblem } from '@iplik/api-client';
import { useMutation } from '@tanstack/react-query';
import { useEffect, useRef, useState } from 'react';
import { loadHelpChat, saveHelpChat } from './helpChatStorage';

/** Modele giden önceki mesaj sayısı; sunucu da en çok bu kadarını alır. */
const MAX_HISTORY = 10;

export type HelpChatError = 'rateLimited' | 'unavailable';
export type ChatEntry = AssistantMessage & { id: number };

/**
 * Yardım sohbeti: mesajları tutar, soruyu sorar. Sohbet pencere kapanınca ve sayfa yenilenince korunur (bu sekmede);
 * çıkışta silinir.
 */
export function useHelpChat() {
  const [entries, setEntries] = useState<readonly ChatEntry[]>(loadHelpChat);
  const [error, setError] = useState<HelpChatError | null>(null);
  const nextId = useRef(entries.reduce((highest, entry) => Math.max(highest, entry.id + 1), 0));
  useEffect(() => {
    saveHelpChat(entries);
  }, [entries]);
  const mutation = useMutation({
    mutationFn: ({
      question,
      history,
    }: {
      question: string;
      history: readonly AssistantMessage[];
    }) => askAssistant(question, history),
  });

  function append(role: AssistantMessage['role'], text: string): void {
    const id = nextId.current++;
    setEntries((current) => [...current, { id, role, text }]);
  }

  function send(question: string): void {
    const history = entries.slice(-MAX_HISTORY).map(({ role, text }) => ({ role, text }));
    setError(null);
    append('user', question);
    mutation.mutate(
      { question, history },
      {
        onSuccess: (response) => {
          append('assistant', response.answer);
        },
        onError: (failure) => {
          setError(toProblem(failure).code === 'HELP_RATE_LIMITED' ? 'rateLimited' : 'unavailable');
        },
      },
    );
  }

  return { entries, error, isSending: mutation.isPending, send };
}
