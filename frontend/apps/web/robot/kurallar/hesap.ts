/**
 * İş kuralları ve hesap formülleri: robot sonuçları bu formüllerle bağımsız olarak hesaplayıp sistemin gösterdiğiyle
 * karşılaştırır. Kural değişince (ör. yeni fire formülü) yalnızca burası güncellenir.
 */

/** Tek kayıtta girilebilecek en büyük kilo (backend `Kilograms.MAX_ENTRY_KG`, arayüz `parseKg`). */
export const KILO_UST_SINIR = 100_000;
/** Irsaliye numarası en çok bu kadar karakter olabilir (`Completion.MAX_WAYBILL_NO_LENGTH`). */
export const IRSALIYE_UZUNLUK = 64;
/** Fire oranı sınırı (yüzde). */
export const FIRE_ARALIK = { en_az: 0, en_cok: 20 } as const;

export type FireGirdi = {
  /** G: fasoncuya gönderilen net kg */
  gonderilen: number;
  /** D: dönen iplik */
  donen: number;
  /** I: haklı itiraz kg */
  itiraz: number;
  /** K: gelen kumaş */
  kumas: number;
  /** f: fire oranı (0,03 = %3) */
  oran: number;
};

/** Fire dosyası (FireCalculation): T = G − D − I; B = T·(1 − f); eksik = B − (K − I); hatalı = max(0, eksik). */
export function fireHesapla(g: FireGirdi) {
  const taban = g.gonderilen - g.donen - g.itiraz;
  const beklenen = taban * (1 - g.oran);
  const netKumas = g.kumas - g.itiraz;
  const eksik = beklenen - netKumas;
  return {
    taban,
    beklenen,
    eksik,
    hatali: Math.max(0, eksik),
    gerceklesenYuzde: taban > 0 ? ((taban - netKumas) / taban) * 100 : 0,
  };
}

/** Merkez kumaş üretiminde stoktan düşen iplik: kumaş / (1 − fire). */
export const uretimIplikDusumu = (kumasKg: number, fireOrani: number) => kumasKg / (1 - fireOrani);

/** Sayfa açılış süresi (ms) ve tek bir API yanıtının süresi için uyarı eşikleri (yerel ortam). */
export const SAYFA_SURESI_MS = 5000;
export const API_SURESI_MS = 3000;
/** Dokunmatik ekranda bir düğme ya da bağlantının en az kenar uzunluğu (px). */
export const DOKUNMA_HEDEFI_PX = 40;

/** Binde bir kilo toleransıyla eşitlik (sunucu 3 ondalık yuvarlar). */
export const kiloEsit = (a: number, b: number) => Math.abs(a - b) < 0.002;

/** Türkçe kilo biçimi: 1.982,5 kg */
export const kiloMetni = (n: number) =>
  `${new Intl.NumberFormat('tr-TR', { maximumFractionDigits: 3 }).format(n)} kg`;
