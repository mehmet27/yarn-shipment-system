import { useTranslation } from '@iplik/i18n';
import { Alert, SimpleGrid, Stack, Text } from '@iplik/ui';
import { compareWeighing, formatKgText, formatSignedKg } from '@iplik/units';

type Props = { recipeKg: string; measuredKg: string; toleranceKg: string | undefined };

/**
 * Reçete toplamı ile kantarın tartısı yan yana: tutuyorsa yeşil "yola çıkabilir", tutmuyorsa kırmızı fark. Asıl karar
 * sunucudadır; bu yalnızca anlık yol göstericidir.
 */
export function RecipeWeightStatus({ recipeKg, measuredKg, toleranceKg }: Props) {
  const { t } = useTranslation();
  const comparison = compareWeighing(recipeKg, measuredKg, toleranceKg);
  const isFitting = comparison !== undefined && !comparison.exceedsTolerance;
  const items = [
    { key: 'measured', value: formatKgText(measuredKg) },
    { key: 'recipe', value: formatKgText(recipeKg) },
    {
      key: 'diff',
      value: comparison === undefined ? '—' : formatSignedKg(comparison.diffKg),
    },
  ];
  return (
    <Alert
      color={isFitting ? 'teal' : 'red'}
      title={t(isFitting ? 'recipeEdit.status.fits' : 'recipeEdit.status.notFits')}
      data-testid="recipe-weight-status"
    >
      <Stack gap="xs">
        <SimpleGrid cols={{ base: 1, xs: 3 }} spacing="sm">
          {items.map((item) => (
            <Stack key={item.key} gap={2}>
              <Text size="sm" c="dimmed">
                {t(`recipeEdit.status.${item.key}`)}
              </Text>
              <Text fw={700}>{item.value}</Text>
            </Stack>
          ))}
        </SimpleGrid>
        <Text size="sm">
          {isFitting
            ? t('recipeEdit.status.fitsHint')
            : t('recipeEdit.status.notFitsHint', {
                tolerance: formatKgText(toleranceKg ?? '0.000'),
              })}
        </Text>
      </Stack>
    </Alert>
  );
}
