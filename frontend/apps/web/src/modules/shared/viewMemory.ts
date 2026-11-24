/**
 * Kişinin bir ekranda en son seçtiği görünüm (sekme, dönem, süzgeç) bu tarayıcıda saklanır. Yalnızca arayüz tercihidir;
 * sevkiyat ya da kişi verisi yazılmaz. Anahtar kullanıcıya özeldir: ortak bilgisayarda biri diğerinin seçimini görmez.
 */
const STORAGE_PREFIX = 'iplik.view';

function storageKey(userId: string, screen: string): string {
  return `${STORAGE_PREFIX}.${userId}.${screen}`;
}

/** Saklanan sorgu metni (`durum=DRAFT`); yoksa ya da okunamıyorsa null. */
export function loadView(userId: string, screen: string): string | null {
  try {
    return window.localStorage.getItem(storageKey(userId, screen));
  } catch {
    return null;
  }
}

/** Boş görünüm kaydı siler; saklanamazsa (özel pencere, kota) sessizce geçilir. */
export function saveView(userId: string, screen: string, search: string): void {
  try {
    if (search === '') window.localStorage.removeItem(storageKey(userId, screen));
    else window.localStorage.setItem(storageKey(userId, screen), search);
  } catch {
    // Tercih saklanamadı; ekran bu oturumda yine çalışır.
  }
}
