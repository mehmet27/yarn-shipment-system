import { formatDateTime, useTranslation } from '@iplik/i18n';
import { EmptyState, Paper, Stack, Text } from '@iplik/ui';
import { usePermission } from '@/modules/auth';
import { QueryState } from '../components/QueryState';
import { formatKgText } from './format';
import { useYarnReturns } from './hooks';
import { YarnReturnForm } from './YarnReturnForm';

/** Fasoncudan depoya iplik dönüşü; kaydı yalnızca depo girer (YARN_RETURN_RECORD). Eski "gelen iplik" kayıtları listede görünür. */
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
            <EmptyState title={t('settlement.yarn.empty')} />
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
