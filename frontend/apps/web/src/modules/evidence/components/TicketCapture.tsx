import type { EvidenceType } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Paper, Stack, Text } from '@iplik/ui';
import { useEffect } from 'react';
import { useTicketPhoto } from '../hooks/useTicketPhoto';
import type { TicketOutcome } from '../ticketTypes';
import { uploadErrorKey } from '../uploadErrors';
import { CameraButton } from './CameraButton';
import { TicketPreview } from './TicketPreview';
import { TicketStatus } from './TicketStatus';

export type TicketCaptureProps = {
  shipmentId: string;
  evidenceType: Extract<EvidenceType, 'DISPATCH_SCALE' | 'DELIVERY_SCALE'>;
  outcome: TicketOutcome | undefined;
  onOutcome: (outcome: TicketOutcome | undefined) => void;
  /** Fotoğraf seçildi ama henüz gönderilmedi mi; form bunu kaydetmeden önce uyarmak için kullanır. */
  onPendingChange?: (pending: boolean) => void;
};

/** Kantar fişi çekimi: kamera → önizleme → gönder. Fişin yalnızca fotoğrafı saklanır; sistem fişi okumaz. */
export function TicketCapture({
  shipmentId,
  evidenceType,
  outcome,
  onOutcome,
  onPendingChange,
}: TicketCaptureProps) {
  const { t } = useTranslation();
  const flow = useTicketPhoto(shipmentId, evidenceType, onOutcome);
  const { mutation, file } = flow;
  const isPending = file !== undefined;
  useEffect(() => {
    onPendingChange?.(isPending);
  }, [isPending, onPendingChange]);
  const isFirst = outcome === undefined && file === undefined;
  return (
    <Paper withBorder p="md" style={{ borderStyle: 'dashed' }}>
      <Stack>
        <Text fw={600}>{t('ticket.title')}</Text>
        {isFirst && (
          <Text size="sm" c="dimmed">
            {t('ticket.guide')}
          </Text>
        )}
        {file !== undefined && (
          <TicketPreview
            file={file}
            issues={flow.issues}
            isSending={mutation.isPending}
            onSend={() => {
              flow.send(file);
            }}
          />
        )}
        <TicketStatus problem={flow.problem} outcome={outcome} />
        {mutation.isError && <Alert color="red">{t(uploadErrorKey(mutation.error))}</Alert>}
        <CameraButton
          label={t(
            outcome === undefined && file === undefined ? 'ticket.capture' : 'ticket.retake',
          )}
          testId="ticket-input"
          variant={isFirst ? 'filled' : 'default'}
          onPick={flow.pick}
        />
      </Stack>
    </Paper>
  );
}
