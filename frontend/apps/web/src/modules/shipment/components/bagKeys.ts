/** Tek atamada en fazla bu kadar çuval anahtarı gönderilir. */
export const MAX_BAG_KEYS = 2000;

/** Okutulan/yazılan anahtarı büyük harfe çevirip listeye ekler; boş, tekrar ya da sınır aşımı eklenmez. */
export function addBagKey(keys: readonly string[], input: string): readonly string[] {
  const key = input.trim().toUpperCase();
  if (key === '' || keys.includes(key) || keys.length >= MAX_BAG_KEYS) return keys;
  return [...keys, key];
}
