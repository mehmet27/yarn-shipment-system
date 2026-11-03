import type { DiscrepancySettingsView } from '@iplik/api-client';
import { formatDateTime, useTranslation } from '@iplik/i18n';
import { SimpleGrid, Stack, Table, Title } from '@iplik/ui';
import { useActorLabel } from '@/modules/shipment';

const pctText = (value: string) => `%${value.replace('.', ',')}`;

/** Her kuralın sürümleri en yeniden eskiye; açık uçlu sürüm halen geçerli olandır. */
export function SettingsHistory({ settings }: { settings: DiscrepancySettingsView }) {
  const { t } = useTranslation();
  const actorLabel = useActorLabel();
  const period = (validFrom: string, validTo: string | undefined) =>
    `${formatDateTime(validFrom)} – ${validTo === undefined ? t('discrepancy.settings.current') : formatDateTime(validTo)}`;
  return (
    <SimpleGrid cols={{ base: 1, md: 2 }}>
      <Stack gap="xs">
        <Title order={4}>{t('discrepancy.settings.toleranceHistory')}</Title>
        <Table aria-label={t('discrepancy.settings.toleranceHistory')}>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>{t('discrepancy.settings.bands')}</Table.Th>
              <Table.Th>{t('discrepancy.settings.period')}</Table.Th>
              <Table.Th>{t('discrepancy.settings.setBy')}</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {settings.toleranceHistory.map((version) => (
              <Table.Tr key={version.validFrom}>
                <Table.Td>
                  {pctText(version.greenMaxPct)} / {pctText(version.yellowMaxPct)}
                </Table.Td>
                <Table.Td>{period(version.validFrom, version.validTo)}</Table.Td>
                <Table.Td>{actorLabel(version.setBy)}</Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      </Stack>
      <Stack gap="xs">
        <Title order={4}>{t('discrepancy.settings.timeLimitHistory')}</Title>
        <Table aria-label={t('discrepancy.settings.timeLimitHistory')}>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>{t('discrepancy.settings.inTransitMaxHours')}</Table.Th>
              <Table.Th>{t('discrepancy.settings.period')}</Table.Th>
              <Table.Th>{t('discrepancy.settings.setBy')}</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {settings.timeLimitHistory.map((version) => (
              <Table.Tr key={version.validFrom}>
                <Table.Td>
                  {t('discrepancy.settings.hours', { count: version.inTransitMaxHours })}
                </Table.Td>
                <Table.Td>{period(version.validFrom, version.validTo)}</Table.Td>
                <Table.Td>{actorLabel(version.setBy)}</Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      </Stack>
    </SimpleGrid>
  );
}
