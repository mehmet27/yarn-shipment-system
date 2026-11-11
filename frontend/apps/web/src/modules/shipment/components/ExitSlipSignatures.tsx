import { useTranslation } from '@iplik/i18n';
import { Box, SimpleGrid, Stack, Text } from '@iplik/ui';

const SIGNATURES = ['deliveredBy', 'driver', 'receivedBy'] as const;

/** Teslim eden, şoför ve teslim alan imza alanları. */
export function ExitSlipSignatures() {
  const { t } = useTranslation();
  return (
    <SimpleGrid cols={3} pt="xl" mt="xl">
      {SIGNATURES.map((key) => (
        <Stack key={key} gap="xl" align="center">
          <Text fw={600}>{t(`shipment.exitSlip.${key}`)}</Text>
          <Box w="80%" style={{ borderBottom: '1px solid var(--mantine-color-gray-6)' }} />
        </Stack>
      ))}
    </SimpleGrid>
  );
}
