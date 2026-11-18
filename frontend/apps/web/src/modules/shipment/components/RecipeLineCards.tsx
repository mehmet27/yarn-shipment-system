import type { RecipeLineView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { formatKgText } from '@iplik/units';
import { Group, Paper, Stack, Text } from '@iplik/ui';
import { specText, yarnLabel } from './recipeText';

/** Dar ekranda reçete satırları: iplik ve net kg üstte, ayrıntılar altta; yatay kaydırma gerekmez. */
export function RecipeLineCards({
  lines,
  total,
}: {
  lines: readonly RecipeLineView[];
  total: string;
}) {
  const { t } = useTranslation();
  return (
    <Paper withBorder style={{ overflow: 'hidden' }}>
      {lines.map((line, index) => (
        <Stack
          key={`${yarnLabel(line)}-${String(index)}`}
          gap={2}
          p="sm"
          style={{ borderTop: index === 0 ? undefined : '1px solid var(--app-line-soft)' }}
        >
          <Group justify="space-between" wrap="nowrap" align="flex-start">
            <Text fw={600}>{yarnLabel(line)}</Text>
            <Text fw={600} ta="right" style={{ whiteSpace: 'nowrap' }}>
              {formatKgText(line.netKg)}
            </Text>
          </Group>
          <Text size="sm" c="dimmed">
            {t('shipment.file.recipe.fabricType')}: {line.fabricType ?? '—'}
          </Text>
          <Text size="sm" c="dimmed">
            {t('shipment.file.recipe.spec')}: {specText(line, t)}
          </Text>
          <Text size="sm" c="dimmed">
            {t('shipment.file.recipe.lot')}: {line.lotNo ?? line.lotId ?? '—'}
          </Text>
        </Stack>
      ))}
      <Group
        justify="space-between"
        p="sm"
        bg="var(--app-inset)"
        style={{ borderTop: '1px solid var(--app-line)' }}
      >
        <Text fw={600}>{t('shipment.file.recipe.total')}</Text>
        <Text fw={600}>{total}</Text>
      </Group>
    </Paper>
  );
}
