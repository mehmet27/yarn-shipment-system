import { describe, expect, it } from 'vitest';
import { normalizeLabel, validateLabel } from './recordDraft';

describe('validateLabel', () => {
  it('boş ve yalnızca boşluktan oluşan metni reddeder', () => {
    expect(validateLabel('subcontractor', '   ')).toBe('labelRequired');
  });

  it('plaka en fazla 20 karakter olabilir', () => {
    expect(validateLabel('vehicle', 'A'.repeat(21))).toBe('labelTooLong');
    expect(validateLabel('vehicle', '34 ABC 123')).toBeUndefined();
  });

  it('TR plaka desenine uymayan plakayı reddeder', () => {
    expect(validateLabel('vehicle', 'abc')).toBe('invalidPlate');
    expect(validateLabel('vehicle', '00 A 1234')).toBe('invalidPlate');
    expect(validateLabel('vehicle', '82 A 1234')).toBe('invalidPlate');
    expect(validateLabel('vehicle', '34 ABCD 12')).toBe('invalidPlate');
    expect(validateLabel('vehicle', '34 A 1')).toBe('invalidPlate');
  });

  it('küçük harfli ve boşluklu geçerli plakayı kabul eder', () => {
    expect(validateLabel('vehicle', '06 a 1234')).toBeUndefined();
    expect(validateLabel('vehicle', '81zz99')).toBeUndefined();
  });

  it('plaka dışındaki türlerde desen aranmaz', () => {
    expect(validateLabel('subcontractor', 'abc')).toBeUndefined();
  });
});

describe('normalizeLabel', () => {
  it('plakayı boşluksuz büyük harfe çevirir; i harfi I olur', () => {
    expect(normalizeLabel('vehicle', ' 34 abi 123 ')).toBe('34ABI123');
  });

  it('diğer türlerde yalnızca kenar boşluklarını siler', () => {
    expect(normalizeLabel('subcontractor', '  Aydın  Örme ')).toBe('Aydın  Örme');
  });
});
