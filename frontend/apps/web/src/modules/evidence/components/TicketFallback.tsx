import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Select, Stack } from '@iplik/ui';
import { useState } from 'react';
import type { UnverifiedReason } from '@iplik/api-client';
import { toUnverifiedReason, UNVERIFIED_REASONS } from '../ticketTypes';
import { CameraButton } from './CameraButton';

export type TicketFallbackProps = {
  /** Kantar ekranı fotoğrafı yükleniyor. */
  isSending: boolean;
  /** Gerekçe kaydediliyor / kaydedilemedi. */
  isDeclaring: boolean;
  isFailed: boolean;
  onDisplayPhoto: (file: File) => void;
  onReason: (reason: UnverifiedReason) => void;
};

/** Üç denemeden sonra: kantar ekranını çek ya da gerekçe seçip devam et ("K0 doğrulanamadı"). */
export function TicketFallback({
  isSending,
  isDeclaring,
  isFailed,
  onDisplayPhoto,
  onReason,
}: TicketFallbackProps) {
  const { t } = useTranslation();
  const [reason, setReason] = useState<UnverifiedReason>();
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
        value={reason ?? null}
        onChange={(value) => {
          setReason(toUnverifiedReason(value));
        }}
      />
      <Button
        variant="default"
        disabled={reason === undefined || isSending}
        loading={isDeclaring}
        onClick={() => {
          if (reason !== undefined) onReason(reason);
        }}
      >
        {t('ticket.fallback.continue')}
      </Button>
      {isFailed && <Alert color="red">{t('ticket.fallback.failed')}</Alert>}
    </Stack>
  );
}
