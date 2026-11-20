import { useTranslation } from '@iplik/i18n';
import { PageHero, Paper, Select, SimpleGrid, Stack, Title } from '@iplik/ui';
import { useMasterOptions } from '@/modules/shipment';
import { QueryState } from '@/modules/shared';
import { useState } from 'react';
import { BalanceMatrix } from '../components/BalanceMatrix';
import { MovementList } from '../components/MovementList';
import { useBalances } from '../hooks/useBalances';

/** Fasoncu bakiyesi (BALANCE_VIEW): iplik cinsi x fasoncu matrisi ve hareket listesi. */
export function BalancesPage() {
  const { t } = useTranslation();
  const [subcontractorId, setSubcontractorId] = useState<string>();
  const [yarnType, setYarnType] = useState<string>();
  const [page, setPage] = useState(1);
  const subcontractors = useMasterOptions('subcontractor', '');
  const query = useBalances(subcontractorId);
  const yarnTypes = [...new Set(query.data?.items.map((row) => row.yarnType))].sort();
  return (
    <Stack>
      <PageHero
        title={t('settlement.balances.title')}
        subtitle={t('settlement.balances.subtitle')}
      />
      <Paper withBorder p="md">
        <SimpleGrid cols={{ base: 1, sm: 2 }}>
          <Select
            label={t('settlement.balances.subcontractor')}
            placeholder={t('settlement.balances.allSubcontractors')}
            data={subcontractors.data ?? []}
            value={subcontractorId ?? null}
            clearable
            onChange={(value) => {
              setSubcontractorId(value ?? undefined);
              setPage(1);
            }}
          />
          <Select
            label={t('settlement.balances.yarnType')}
            placeholder={t('settlement.balances.allYarns')}
            data={yarnTypes}
            value={yarnType ?? null}
            clearable
            onChange={(value) => {
              setYarnType(value ?? undefined);
              setPage(1);
            }}
          />
        </SimpleGrid>
      </Paper>
      <QueryState query={query}>{(view) => <BalanceMatrix view={view} />}</QueryState>
      <Title order={2}>{t('settlement.balances.movements')}</Title>
      <MovementList
        filter={{
          page,
          ...(subcontractorId === undefined ? {} : { subcontractorId }),
          ...(yarnType === undefined ? {} : { yarnType }),
        }}
        onPage={setPage}
      />
    </Stack>
  );
}
