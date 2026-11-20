const DATE_TIME_FORMAT = new Intl.DateTimeFormat('tr-TR', {
    dateStyle: 'short',
    timeStyle: 'short',
    timeZone: 'Europe/Istanbul',
});
/** FE-API-09: API'den gelen ISO 8601 UTC değeri İstanbul saatiyle gösterir. */
export function formatDateTime(isoUtc) {
    return DATE_TIME_FORMAT.format(new Date(isoUtc));
}
/** Saatsiz gün (`2026-11-20`) Türkçe gösterilir; saat dilimi kaymasın diye parçalar elle birleştirilir. */
export function formatDate(isoDate) {
    const [year, month, day] = isoDate.split('-');
    return `${day ?? ''}.${month ?? ''}.${year ?? ''}`;
}
/** Bugünün günü (`YYYY-MM-DD`), fabrikanın saatiyle (İstanbul). */
export function todayIsoDate(now = new Date()) {
    const parts = new Intl.DateTimeFormat('en-CA', { timeZone: 'Europe/Istanbul' }).format(now);
    return parts;
}
