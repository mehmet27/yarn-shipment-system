/**
 * Veri değiştiren düğmeler ve bunları deneyen senaryolar. Gezgin (05-tum-ekranlar) bu düğmeleri tıklamaz; burada
 * yazılı değilse "kapsam açığı" raporlar. Yeni bir düğme eklendiğinde ilgili senaryoyu yazıp buraya ekleyin.
 * Anahtar: `rota|düğme adı` (rota kimlikleri `<id>` olarak kısaltılır) ya da `*|düğme adı` (her sayfa).
 */
export const KAPSANAN: Record<string, string> = {
  '/panel/merkez-stok|İplik girişini kaydet': '02-formlar, 03-akis',
  '/panel/merkez-stok|Kumaşı kaydet': '02-formlar, 03-akis',
  '/panel/sevkiyat/yeni|Taslak oluştur': '02-formlar, 03-akis',
  '/panel/sevkiyat/<id>|Sevkiyatı tamamla': '03-akis',
  '/panel/sevkiyat/<id>|İptal et': '09-sevkiyat-islemleri',
};
