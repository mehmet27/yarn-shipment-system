const FORMATTER = new Intl.DateTimeFormat('tr-TR', { dateStyle: 'short', timeStyle: 'short' });

/** Sunucudaki ISO zamanını kullanıcının yerel saatiyle gösterir. */
export function formatDateTime(iso: string): string {
  return FORMATTER.format(new Date(iso));
}
