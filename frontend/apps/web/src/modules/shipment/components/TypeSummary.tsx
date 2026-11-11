import { useTranslation } from '@iplik/i18n';
import { Text } from '@iplik/ui';

/** Ambalaj türüne göre adetler: "3 Çuval". Tür bilgisi yoksa hiçbir şey çizilmez. */
export function TypeSummary({ types }: { types: readonly string[] }) {
  const { t } = useTranslation();
  const counts = new Map<string, number>();
  for (const type of types) {
    counts.set(type, (counts.get(type) ?? 0) + 1);
  }
  if (counts.size === 0) {
    return null;
  }
  const summary = [...counts]
    .map(([type, count]) => `${String(count)} ${t(`bag.packageTypes.${type}`)}`)
    .join(', ');
  return (
    <Text size="sm" c="dimmed">
      {t('shipment.bags.byType', { summary })}
    </Text>
  );
}
