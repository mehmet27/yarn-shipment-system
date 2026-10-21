import { describe, expect, it } from 'vitest';
import { formatDateTime } from './dateTime';

describe('formatDateTime', () => {
  it('UTC zamanı İstanbul saatine çevirir', () => {
    expect(formatDateTime('2026-10-20T08:15:30Z')).toBe('20.10.2026 11:15');
  });
});
