import { formatDateTime, useTranslation } from '@iplik/i18n';
import { Stack, Table, Text } from '@iplik/ui';
import { formatSignedKg, ListPagination, QueryState, useActorLabel } from '@/modules/shipment';
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
  const query = useBalanceMovements(filter);
  return (
    <QueryState query={query}>
      {({ items, page }) =>
        items.length === 0 ? (
          <Text c="dimmed">{t('settlement.balances.movementsEmpty')}</Text>
        ) : (
          <Stack>
            <Table.ScrollContainer minWidth={560}>
              <Table striped aria-label={t('settlement.balances.movements')}>
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>{t('settlement.balances.date')}</Table.Th>
                    <Table.Th>{t('settlement.balances.kind')}</Table.Th>
                    <Table.Th>{t('settlement.balances.yarnType')}</Table.Th>
                    <Table.Th>{t('settlement.balances.kg')}</Table.Th>
                    <Table.Th>{t('settlement.balances.reason')}</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {items.map((move) => (
                    <Table.Tr key={move.id}>
                      <Table.Td>{formatDateTime(move.occurredAt)}</Table.Td>
                      <Table.Td>{t(`settlement.balances.kinds.${move.kind}`)}</Table.Td>
                      <Table.Td>{move.yarnType}</Table.Td>
                      <Table.Td>{formatSignedKg(move.netKg)}</Table.Td>
                      <Table.Td>{move.reason ?? actorLabel(move.createdBy)}</Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            </Table.ScrollContainer>
            {page.totalPages > 1 && (
              <ListPagination total={page.totalPages} value={filter.page} onChange={onPage} />
            )}
          </Stack>
        )
      }
    </QueryState>
  );
}
