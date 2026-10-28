import type { TicketReadingView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Text } from '@iplik/ui';

/** Okunamadı ya da tutarsız: nedenleri gösterir ve yeniden çekmesini ister. */
export function TicketUnreadable({ reading }: { reading: TicketReadingView }) {
  const { t } = useTranslation();
  return (
    <Alert color="red" title={t('ticket.unreadable')}>
      {reading.problems.map((problem) => (
        <Text key={problem} size="sm">
          {t(`ticket.problems.${problem}`)}
        </Text>
      ))}
    </Alert>
  );
}
