import type { LotView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Badge, Button, Group, Paper, Stack, Text } from '@iplik/ui';
import { useSetLotActive } from '../hooks/useLotMutations';
import { LockLotButton } from './LockLotButton';
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
  const mutation = useSetLotActive();
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
            site: lot.siteName,
            date: formatDay(lot.productionDate),
          })}
        </Text>
        <Text size="sm" c="dimmed">
          {lot.yarnSupplierName === undefined
            ? t('lots.noSupplier')
            : t('lots.supplier', { name: lot.yarnSupplierName })}
        </Text>
        {!isLocked && lot.active && <LockLotButton lot={lot} />}
        <Group grow>
          {!isLocked && (
            <Button
              variant="default"
              onClick={() => {
                onEdit(lot);
              }}
            >
              {t('masterData.edit')}
            </Button>
          )}
          <Button
            variant="light"
            color={lot.active ? 'red' : 'green'}
            loading={mutation.isPending}
            onClick={() => {
              mutation.mutate({ id: lot.id, isActive: !lot.active });
            }}
          >
            {t(lot.active ? 'masterData.deactivate' : 'masterData.activate')}
          </Button>
        </Group>
      </Stack>
    </Paper>
  );
}
