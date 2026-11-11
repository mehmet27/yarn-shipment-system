import { useTranslation } from '@iplik/i18n';
import { Loader, Stack, Table, Text } from '@iplik/ui';
import { useTareStandards } from '../hooks/useTareStandards';
import { TareStandardForm } from './TareStandardForm';

/** Yönetici "Standart dara": ambalaj başına standart dara ile izin verilen sapma. */
export function TareStandardsPanel() {
  const { t } = useTranslation();
  const standards = useTareStandards();
  const rows = standards.data ?? [];
  return (
    <Stack>
      <Text c="dimmed">{t('masterData.tareStandard.intro')}</Text>
      <TareStandardForm />
      {standards.isPending && <Loader size="sm" />}
      {standards.isSuccess && rows.length === 0 && (
        <Text c="dimmed">{t('masterData.tareStandard.empty')}</Text>
      )}
      {rows.length > 0 && (
        <Table>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>{t('masterData.tareStandard.packagingType')}</Table.Th>
              <Table.Th ta="right">{t('masterData.tareStandard.standardTareKg')}</Table.Th>
              <Table.Th ta="right">{t('masterData.tareStandard.maxDeviationPct')}</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {rows.map((row) => (
              <Table.Tr key={row.id}>
                <Table.Td>{t(`masterData.tareStandard.types.${row.packagingType}`)}</Table.Td>
                <Table.Td ta="right">{row.standardTareKg}</Table.Td>
                <Table.Td ta="right">{row.maxDeviationPct}</Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      )}
    </Stack>
  );
}
