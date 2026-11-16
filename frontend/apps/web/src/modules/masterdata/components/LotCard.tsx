import type { LotView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Badge, Group, Paper, Stack, Text } from '@iplik/ui';
import { LockLotButton } from './LockLotButton';
import { LotActions } from './LotActions';
import { LotLockState } from './LotLockState';

export type LotCardProps = { lot: LotView; onEdit: (lot: LotView) => void };

const DATE_FORMATTER = new Intl.DateTimeFormat('tr-TR', { dateStyle: 'medium' });

/** Üretim günü saat dilimi taşımaz; yerel gece yarısı olarak okunur. */
function formatDay(day: string): string {
  return DATE_FORMATTER.format(new Date(`${day}T00:00:00`));
}

/** Parti kartı: kilitli partide yalnızca pasife alma kalır; kilitsizde düzenle ve kilitle. */
export function LotCard({ lot, onEdit }: LotCardProps) {
  const { t } = useTranslation();
  const isLocked = lot.qualityLockedAt !== undefined;
  return (
    <Paper withBorder p="md" role="group" aria-label={lot.lotNo}>
      <Stack gap="sm">
        <Group justify="space-between" wrap="nowrap">
          <Text fw={600} {...(lot.active ? {} : { c: 'dimmed' })}>
            {lot.lotNo}
          </Text>
          <Group gap="xs">
            <LotLockState lot={lot} />
            {!lot.active && <Badge color="gray">{t('masterData.inactive')}</Badge>}
          </Group>
        </Group>
        <Text size="sm">
          {lot.productCode} · {lot.productName}
        </Text>
        <Text size="sm" c="dimmed">
          {t('lots.summary', {
            grade: `${lot.qualityGradeCode} · ${lot.qualityGradeName}`,
            date: formatDay(lot.productionDate),
          })}
        </Text>
        <Text size="sm" c="dimmed">
          {lot.yarnSupplierName === undefined
            ? t('lots.noSupplier')
            : t('lots.supplier', { name: lot.yarnSupplierName })}
        </Text>
        {!isLocked && lot.active && <LockLotButton lot={lot} />}
        <LotActions lot={lot} isLocked={isLocked} onEdit={onEdit} />
      </Stack>
    </Paper>
  );
}
