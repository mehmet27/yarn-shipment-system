import type { TicketReadingView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Stack, Text } from '@iplik/ui';
import { formatKg, parseKg } from '@iplik/units';

/** Fişten okunan kilolar; yeniden çekim gerektirmeyen sorunlar yalnızca bilgi olarak listelenir. */
function format(value: number): string {
  const kg = parseKg(String(value));
  return kg === undefined ? String(value) : formatKg(kg);
}

export function TicketReadingResult({ reading }: { reading: TicketReadingView }) {
  const { t } = useTranslation();
  const { grossKg, tareKg, netKg } = reading;
  return (
    <Alert color="green" title={t('ticket.readTitle')}>
      <Stack gap="xs">
        {grossKg !== undefined && tareKg !== undefined && netKg !== undefined && (
          <Text>
            {t('ticket.readValues', {
              gross: format(grossKg),
              tare: format(tareKg),
              net: format(netKg),
            })}
          </Text>
        )}
        {reading.problems.map((problem) => (
          <Text key={problem} size="sm">
            {t(`ticket.problems.${problem}`)}
          </Text>
        ))}
      </Stack>
    </Alert>
  );
}
