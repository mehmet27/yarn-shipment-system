import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Group, Paper, Stack, Text } from '@iplik/ui';
import { calculateFire, formatKgText, isKilogram, parseFireRatePct } from '@iplik/units';
import type { CreateDraft, CreateFieldErrors } from './createDraft';
import { totalNetKg } from './createDraft';
import { FireRateField } from './FireRateField';
import { useOrderFormPdf } from './useOrderFormPdf';

export type CreateSummaryProps = {
  draft: CreateDraft;
  onChange: (draft: CreateDraft) => void;
  errors: CreateFieldErrors;
  defaultFirePct: string;
  failureKey: string | undefined;
  isSubmitting: boolean;
  onSubmit: () => void;
};

const ZERO_KG = '0.000';

/** Beklenen kumaş = toplam net × (1 − fire) (analiz 8.1); fire ya da toplam geçersizse gösterilmez. */
function expectedFabric(total: string, firePct: string): string | undefined {
  const fireRate = parseFireRatePct(firePct);
  if (fireRate === undefined || !isKilogram(total) || !isKilogram(ZERO_KG)) return undefined;
  return calculateFire({
    sentKg: total,
    returnedKg: ZERO_KG,
    claimKg: ZERO_KG,
    fabricKg: ZERO_KG,
    fireRate,
  }).expectedFabricKg;
}

/** Toplam, fire, beklenen kumaş ve gönderme düğmesi; geniş ekranda sağda yapışkan durur. */
export function CreateSummary({
  draft,
  onChange,
  errors,
  defaultFirePct,
  failureKey,
  isSubmitting,
  onSubmit,
}: CreateSummaryProps) {
  const { t } = useTranslation();
  const downloadPdf = useOrderFormPdf();
  const total = totalNetKg(draft.lines);
  const firePct = draft.fireRatePct ?? defaultFirePct;
  const fabric = expectedFabric(total, firePct);
  return (
    <Paper withBorder p="md" radius="md" pos="sticky" top={16}>
      <Stack gap="sm">
        <Text fw={700}>{t('create.sections.summary')}</Text>
        <Group justify="space-between">
          <Text c="dimmed">{t('create.summary.total')}</Text>
          <Text fw={700} data-testid="recipe-total">
            {t('create.recipe.total', { total: formatKgText(total) })}
          </Text>
        </Group>
        <FireRateField
          value={firePct}
          errorCode={errors.fireRate}
          onChange={(fireRatePct) => {
            onChange({ ...draft, fireRatePct });
          }}
        />
        <Group justify="space-between">
          <Text c="dimmed">{t('create.summary.expectedFabric')}</Text>
          <Text fw={700}>{fabric === undefined ? '—' : `≈ ${formatKgText(fabric)}`}</Text>
        </Group>
        {failureKey !== undefined && <Alert color="red">{t(failureKey)}</Alert>}
        <Button size="lg" loading={isSubmitting} onClick={onSubmit}>
          {t('create.submit')}
        </Button>
        <Button
          variant="default"
          onClick={() => {
            void downloadPdf(draft);
          }}
        >
          {t('create.pdf.button')}
        </Button>
      </Stack>
    </Paper>
  );
}
