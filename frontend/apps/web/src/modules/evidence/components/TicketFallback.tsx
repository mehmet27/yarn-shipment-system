import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Select, Stack } from '@iplik/ui';
import { useState } from 'react';
import { UNVERIFIED_REASONS } from '../ticketTypes';
import { CameraButton } from './CameraButton';

export type TicketFallbackProps = {
  isSending: boolean;
  onDisplayPhoto: (file: File) => void;
  onReason: (reason: string) => void;
};

/** Üç denemeden sonra: kantar ekranını çek ya da gerekçe seçip devam et ("K0 doğrulanamadı"). */
export function TicketFallback({ isSending, onDisplayPhoto, onReason }: TicketFallbackProps) {
  const { t } = useTranslation();
  const [reason, setReason] = useState<string | null>(null);
  return (
    <Stack>
      <Alert color="orange">{t('ticket.fallback.intro')}</Alert>
      <CameraButton
        label={t('ticket.fallback.display')}
        testId="ticket-display-input"
        isLoading={isSending}
        onPick={onDisplayPhoto}
      />
      <Select
        label={t('ticket.fallback.reason')}
        data={UNVERIFIED_REASONS.map((value) => ({
          value,
          label: t(`ticket.fallback.reasons.${value}`),
        }))}
        value={reason}
        onChange={setReason}
      />
      <Button
        variant="default"
        disabled={reason === null}
        onClick={() => {
          if (reason !== null) onReason(reason);
        }}
      >
        {t('ticket.fallback.continue')}
      </Button>
    </Stack>
  );
}
