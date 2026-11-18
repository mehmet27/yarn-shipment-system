import { useTranslation } from '@iplik/i18n';
import { Alert, Box, Button, Group, Icon, SectionCard, Stack, Text } from '@iplik/ui';
import { calculateFire, formatKgText, isKilogram, parseFireRatePct, parseKg } from '@iplik/units';
import { CentralStockNotice } from '../stock/CentralStockNotice';
import type { CreateDraft } from './createDraft';
import { totalNetKg } from './createDraft';
import { TotalBox } from './TotalBox';
import { useOrderFormPdf } from './useOrderFormPdf';

export type CreateSummaryProps = {
  draft: CreateDraft;
  defaultFirePct: string;
  /** Gönderilemeyen formun nedeni (hazır metin); yoksa uyarı gösterilmez. */
  failure: string | undefined;
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

/** Toplam, beklenen kumaş ve gönderme düğmesi (fire oranı reçete kartında girilir); geniş ekranda sağda yapışkan durur. */
export function CreateSummary({
  draft,
  defaultFirePct,
  failure,
  isSubmitting,
  onSubmit,
}: CreateSummaryProps) {
  const { t } = useTranslation();
  const downloadPdf = useOrderFormPdf();
  const total = totalNetKg(draft.lines);
  const firePct = draft.fireRatePct ?? defaultFirePct;
  const fabric = expectedFabric(total, firePct);
  return (
    <Box pos="sticky" top={16}>
      <SectionCard title={t('create.sections.summary')} icon="list">
        <Stack gap="sm">
          <TotalBox text={t('create.recipe.total', { total: formatKgText(total) })} />
          <Group justify="space-between" wrap="nowrap">
            <Text c="dimmed">
              {t('create.summary.expectedFabric')} (
              {t('create.summary.afterFire', { rate: firePct })})
            </Text>
            <Text fw={600}>{fabric === undefined ? '—' : `≈ ${formatKgText(fabric)}`}</Text>
          </Group>
          <CentralStockNotice
            alwaysShow={false}
            demands={draft.lines.map((line) => ({
              productId: line.yarn?.value,
              yarn: line.yarn?.label ?? '',
              kg: parseKg(line.netKg) ?? '',
            }))}
          />
          {failure !== undefined && <Alert color="red">{failure}</Alert>}
          <Button loading={isSubmitting} leftSection={<Icon name="check" />} onClick={onSubmit}>
            {t('create.submit')}
          </Button>
          <Button
            variant="default"
            leftSection={<Icon name="pdf" />}
            onClick={() => {
              void downloadPdf(draft);
            }}
          >
            {t('create.pdf.button')}
          </Button>
        </Stack>
      </SectionCard>
    </Box>
  );
}
