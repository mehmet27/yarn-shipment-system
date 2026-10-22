import type { ManagedKind } from './kinds';

/** i18n anahtarı (`masterData.errors.*`). */
export type RecordError = 'labelRequired' | 'labelTooLong';

const MAX_LENGTH: Readonly<Record<ManagedKind, number>> = {
  subcontractor: 200,
  driver: 200,
  vehicle: 20,
  site: 200,
};

export function validateLabel(kind: ManagedKind, label: string): RecordError | undefined {
  const trimmed = label.trim();
  if (trimmed === '') return 'labelRequired';
  return trimmed.length > MAX_LENGTH[kind] ? 'labelTooLong' : undefined;
}
