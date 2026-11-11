import { useTranslation } from '@iplik/i18n';
import { Select, SimpleGrid, Stack, Text, Title } from '@iplik/ui';
import { QueryState, useMasterOptions } from '@/modules/shipment';
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
      <Title order={1}>{t('settlement.balances.title')}</Title>
      <Text c="dimmed">{t('settlement.balances.subtitle')}</Text>
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
      <QueryState query={query}>{(view) => <BalanceMatrix view={view} />}</QueryState>
      <Title order={2} size="h3">
        {t('settlement.balances.movements')}
      </Title>
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
