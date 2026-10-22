import { describe, expect, it } from 'vitest';
import { validateLabel } from './recordDraft';

describe('validateLabel', () => {
  it('boş ve yalnızca boşluktan oluşan metni reddeder', () => {
    expect(validateLabel('subcontractor', '   ')).toBe('labelRequired');
  });

  it('plaka en fazla 20 karakter olabilir', () => {
    expect(validateLabel('vehicle', 'A'.repeat(21))).toBe('labelTooLong');
    expect(validateLabel('vehicle', '34 ABC 123')).toBeUndefined();
  });
});
