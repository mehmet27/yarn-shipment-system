import { formatDateTime, useTranslation } from '@iplik/i18n';
import { Paper, Stack, Text } from '@iplik/ui';
import { usePermission } from '@/modules/auth';
import { QueryState } from '../components/QueryState';
import { formatKgText } from './format';
import { useYarnReturns } from './hooks';
import { YarnReturnForm } from './YarnReturnForm';

/** İplik dönüşü ve fasoncudan gelen iplik; kayıt depo/müdür/admin (YARN_RETURN_RECORD). */
export function YarnReturnsTab({ shipmentId }: { shipmentId: string }) {
  const { t } = useTranslation();
  const canRecord = usePermission('YARN_RETURN_RECORD');
  const query = useYarnReturns(shipmentId);
  return (
    <Stack>
      {canRecord && <YarnReturnForm shipmentId={shipmentId} />}
      <QueryState query={query}>
        {(returns) =>
          returns.length === 0 ? (
            <Text c="dimmed">{t('settlement.yarn.empty')}</Text>
          ) : (
            <Stack>
              {returns.map((entry) => (
                <Paper key={entry.id} withBorder p="md" component="article">
                  <Stack gap="xs">
                    <Text fw={600}>
                      {t(`settlement.yarn.directionShort.${entry.direction}`)} · {entry.yarnType} ·{' '}
                      {formatKgText(entry.netKg)}
                    </Text>
                    <Text size="sm" c="dimmed">
                      {formatDateTime(entry.createdAt)}
                      {entry.waybillNo === undefined ? '' : ` · ${entry.waybillNo}`}
                      {entry.lotId === undefined ? '' : ` · ${entry.lotId}`}
                    </Text>
                  </Stack>
                </Paper>
              ))}
            </Stack>
          )
        }
      </QueryState>
    </Stack>
  );
}
