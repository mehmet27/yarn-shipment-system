import { Text } from '@iplik/ui';
import type { FieldValue } from './shipmentViews';

/** Değer kalın ve iri; yokluk nedeni ("Teslimde siz gireceksiniz") soluk ve küçük, veri gibi görünmesin. */
export function FieldValueText({
  field,
  text,
}: {
  field: FieldValue;
  text: (field: FieldValue) => string;
}) {
  return 'value' in field ? (
    <Text fw={700} size="xl">
      {text(field)}
    </Text>
  ) : (
    <Text c="dimmed" size="sm" fs="italic">
      {text(field)}
    </Text>
  );
}
