import { describe, expect, it } from 'vitest';
import { looksLikeImage, preparePhoto, scaledSize } from './preparePhoto';

describe('preparePhoto', () => {
  it('uzun kenarı sınırı aşan fotoğrafı oranı koruyarak küçültür', () => {
    expect(scaledSize(8000, 6000)).toEqual({ width: 2560, height: 1920 });
    expect(scaledSize(6000, 8000)).toEqual({ width: 1920, height: 2560 });
  });

  it('küçük fotoğrafın boyutunu değiştirmez', () => {
    expect(scaledSize(1600, 1200)).toEqual({ width: 1600, height: 1200 });
  });

  it('türü boş gelen kamera dosyasını uzantısından resim sayar; PDF resim değildir', () => {
    expect(looksLikeImage(new File(['x'], 'IMG_0001.HEIC', { type: '' }))).toBe(true);
    expect(looksLikeImage(new File(['x'], 'fis.jpg', { type: 'image/jpeg' }))).toBe(true);
    expect(looksLikeImage(new File(['x'], 'irsaliye.pdf', { type: 'application/pdf' }))).toBe(
      false,
    );
  });

  it('tarayıcı resmi çözemiyorsa (test ortamı) özgün dosyayı aynen döner', async () => {
    const file = new File(['x'], 'fis.heic', { type: 'image/heic' });
    expect(await preparePhoto(file)).toBe(file);
  });
});
