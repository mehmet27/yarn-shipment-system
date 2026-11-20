import { formatDateTime, useTranslation } from '@iplik/i18n';
import { Paper, Stack, Table, Text } from '@iplik/ui';
import { formatSignedKg, useActorLabel, useYarnLabel } from '@/modules/shipment';
import { ListPagination, QueryState } from '@/modules/shared';
import type { MovementFilter } from '../hooks/useBalances';
import { useBalanceMovements } from '../hooks/useBalances';

export function MovementList({
  filter,
  onPage,
}: {
  filter: MovementFilter;
  onPage: (page: number) => void;
}) {
  const { t } = useTranslation();
  const actorLabel = useActorLabel();
  const yarnLabel = useYarnLabel();
  const query = useBalanceMovements(filter);
  return (
    <QueryState query={query}>
      {({ items, page, actorNames }) =>
        items.length === 0 ? (
          <Text c="dimmed">{t('settlement.balances.movementsEmpty')}</Text>
        ) : (
          <Stack>
            <Paper withBorder style={{ overflow: 'hidden' }}>
              <Table.ScrollContainer minWidth={560}>
                <Table aria-label={t('settlement.balances.movements')}>
                  <Table.Thead>
                    <Table.Tr>
                      <Table.Th>{t('settlement.balances.date')}</Table.Th>
                      <Table.Th style={{ whiteSpace: 'nowrap' }}>
                        {t('settlement.balances.kind')}
                      </Table.Th>
                      <Table.Th>{t('settlement.balances.yarnType')}</Table.Th>
                      <Table.Th ta="right">{t('settlement.balances.kg')}</Table.Th>
                      <Table.Th>{t('settlement.balances.reason')}</Table.Th>
                    </Table.Tr>
                  </Table.Thead>
                  <Table.Tbody>
                    {items.map((move) => (
                      <Table.Tr key={move.id}>
                        <Table.Td>{formatDateTime(move.occurredAt)}</Table.Td>
                        <Table.Td style={{ whiteSpace: 'nowrap' }}>
                          {t(`settlement.balances.kinds.${move.kind}`)}
                        </Table.Td>
                        <Table.Td>{yarnLabel(move.yarnType)}</Table.Td>
                        <Table.Td ta="right">{formatSignedKg(move.netKg)}</Table.Td>
                        <Table.Td>
                          {move.reason ?? actorLabel(move.createdBy, actorNames?.[move.createdBy])}
                        </Table.Td>
                      </Table.Tr>
                    ))}
                  </Table.Tbody>
                </Table>
              </Table.ScrollContainer>
            </Paper>
            {page.totalPages > 1 && (
              <ListPagination total={page.totalPages} value={filter.page} onChange={onPage} />
            )}
          </Stack>
        )
      }
    </QueryState>
  );
}
