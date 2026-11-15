import { describe, expect, it } from 'vitest';
import { uploadErrorKey } from './uploadErrors';

/** axios hatasının tanındığı biçim (`isAxiosError`) ve sunucunun problem gövdesi. */
const problem = (code: string) => ({
  isAxiosError: true,
  message: code,
  response: { status: 409, data: { status: 409, code, title: code } },
});

describe('uploadErrorKey', () => {
  it('sevkiyat günü gelmediyse kanıt yükleme hatası kendi mesajına çevrilir', () => {
    expect(uploadErrorKey(problem('SHIPMENT_NOT_DUE'))).toBe('evidence.errors.SHIPMENT_NOT_DUE');
  });

  it('bilinmeyen kod genel mesaja düşer', () => {
    expect(uploadErrorKey(problem('SOMETHING_ELSE'))).toBe('evidence.errors.unknown');
  });
});
