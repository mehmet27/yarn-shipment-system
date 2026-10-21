const DATE_TIME_FORMAT = new Intl.DateTimeFormat('tr-TR', {
  dateStyle: 'short',
  timeStyle: 'short',
  timeZone: 'Europe/Istanbul',
});

/** FE-API-09: API'den gelen ISO 8601 UTC değeri İstanbul saatiyle gösterir. */
export function formatDateTime(isoUtc: string): string {
  return DATE_TIME_FORMAT.format(new Date(isoUtc));
}
