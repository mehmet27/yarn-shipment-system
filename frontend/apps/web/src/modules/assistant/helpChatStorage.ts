import type { ChatEntry } from './useHelpChat';

const STORAGE_KEY = 'yardim-sohbeti';

function isEntry(value: unknown): value is ChatEntry {
  return (
    typeof value === 'object' &&
    value !== null &&
    'id' in value &&
    typeof value.id === 'number' &&
    'role' in value &&
    (value.role === 'user' || value.role === 'assistant') &&
    'text' in value &&
    typeof value.text === 'string'
  );
}

/** Kayıtlı sohbeti okur; yoksa ya da bozuksa boş döner. Tarayıcı depolaması kapalı olabilir. */
export function loadHelpChat(): readonly ChatEntry[] {
  try {
    const parsed: unknown = JSON.parse(window.sessionStorage.getItem(STORAGE_KEY) ?? '[]');
    return Array.isArray(parsed) ? parsed.filter(isEntry) : [];
  } catch {
    return [];
  }
}

/** Sohbeti bu sekmede saklar; sekme kapanınca ya da çıkış yapılınca silinir. */
export function saveHelpChat(entries: readonly ChatEntry[]): void {
  try {
    window.sessionStorage.setItem(STORAGE_KEY, JSON.stringify(entries));
  } catch {
    // Depolama doluysa ya da kapalıysa sohbet yalnızca bellekte kalır.
  }
}

/** Çıkışta çağrılır: bir sonraki kullanıcı öncekinin sorularını görmesin. */
export function clearHelpChat(): void {
  try {
    window.sessionStorage.removeItem(STORAGE_KEY);
  } catch {
    // Depolamaya erişilemiyorsa silinecek bir şey de yoktur.
  }
}
