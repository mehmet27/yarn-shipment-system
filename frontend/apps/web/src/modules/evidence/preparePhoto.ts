/**
 * Telefon fotoğrafını yüklemeye hazırlar: HEIC/WebP gibi türleri, 20 MB'ı aşan 50-100 megapiksel dosyaları ve türü
 * boş gelen kamera dosyalarını cihazda JPEG'e çevirir; büyük fotoğrafı küçültür (mobil veride daha hızlı yüklenir).
 * Çevrilemezse (tarayıcı resmi çözemezse) özgün dosya döner; asıl denetim `fileProblem` ve sunucudadır.
 */
const MAX_EDGE_PX = 2560;
/** Bu boyutun altındaki JPEG/PNG olduğu gibi gider; sıkıştırma kaybı olmaz. */
const BYTES_PER_MB = 1_048_576;
const KEEP_AS_IS_MB = 3;
const KEEP_AS_IS_BYTES = KEEP_AS_IS_MB * BYTES_PER_MB;
const JPEG_QUALITY = 0.85;
const JPEG = 'image/jpeg';
const KEEP_TYPES: readonly string[] = [JPEG, 'image/png'];
const EXTENSION = /\.[^./\\]+$/;
const IMAGE_EXTENSION = /\.(jpe?g|png|webp|gif|bmp|hei[cf])$/i;

export type PhotoSize = { width: number; height: number };

/** En uzun kenarı `MAX_EDGE_PX`'i geçmeyecek biçimde oranı koruyarak yeni boyut; küçükse aynen. */
export function scaledSize(width: number, height: number): PhotoSize {
  const longest = Math.max(width, height);
  if (longest <= MAX_EDGE_PX) return { width, height };
  const ratio = MAX_EDGE_PX / longest;
  return { width: Math.round(width * ratio), height: Math.round(height * ratio) };
}

/** Dosya fotoğraf olabilir mi: türü resim ya da (tür boşsa) uzantısı resim. PDF ve diğerleri dokunulmadan geçer. */
export function looksLikeImage(file: File): boolean {
  if (file.type.startsWith('image/')) return true;
  return file.type === '' && IMAGE_EXTENSION.test(file.name);
}

function jpegName(name: string): string {
  return `${name.replace(EXTENSION, '') || 'foto'}.jpg`;
}

async function toJpeg(bitmap: ImageBitmap, file: File): Promise<File | undefined> {
  const { width, height } = scaledSize(bitmap.width, bitmap.height);
  const canvas = document.createElement('canvas');
  canvas.width = width;
  canvas.height = height;
  const context = canvas.getContext('2d');
  if (context === null) return undefined;
  context.drawImage(bitmap, 0, 0, width, height);
  const blob = await new Promise<Blob | null>((resolve) => {
    canvas.toBlob(resolve, JPEG, JPEG_QUALITY);
  });
  if (blob === null || blob.size === 0) return undefined;
  return new File([blob], jpegName(file.name), { type: JPEG, lastModified: Date.now() });
}

export async function preparePhoto(file: File): Promise<File> {
  if (!looksLikeImage(file) || typeof createImageBitmap !== 'function') return file;
  try {
    const bitmap = await createImageBitmap(file, { imageOrientation: 'from-image' });
    const isSmallEnough = bitmap.width <= MAX_EDGE_PX && bitmap.height <= MAX_EDGE_PX;
    if (KEEP_TYPES.includes(file.type) && file.size <= KEEP_AS_IS_BYTES && isSmallEnough) {
      bitmap.close();
      return file;
    }
    const converted = await toJpeg(bitmap, file);
    bitmap.close();
    return converted ?? file;
  } catch {
    return file;
  }
}
