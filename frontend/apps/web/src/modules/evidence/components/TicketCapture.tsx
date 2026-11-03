import type { EvidenceType } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Paper, Stack, Text } from '@iplik/ui';
import { useUnverifiedFallback } from '../hooks/useDeclareUnverified';
import { useTicketFlow } from '../hooks/useTicketFlow';
import type { TicketOutcome } from '../ticketTypes';
import { MAX_ATTEMPTS } from '../ticketTypes';
import { uploadErrorKey } from '../uploadErrors';
import { CameraButton } from './CameraButton';
import { TicketFallback } from './TicketFallback';
import { TicketPreview } from './TicketPreview';
import { TicketStatus } from './TicketStatus';

export type TicketCaptureProps = {
  shipmentId: string;
  evidenceType: Extract<EvidenceType, 'DISPATCH_SCALE' | 'DELIVERY_SCALE'>;
  outcome: TicketOutcome | undefined;
  onOutcome: (outcome: TicketOutcome | undefined) => void;
};

/**
 * Kantar fişi çekimi: kamera → önizleme → gönder → okuma; en fazla 3 denemeden sonra yedek yol. Gerekçeyle
 * devam edilirse gerekçe son denemenin fişine yazılır ve sunucu "doğrulanamadı" uyuşmazlığı açar (K0).
 */
export function TicketCapture({
  shipmentId,
  evidenceType,
  outcome,
  onOutcome,
}: TicketCaptureProps) {
  const { t } = useTranslation();
  const flow = useTicketFlow(shipmentId, evidenceType, onOutcome);
  const { declare, onReason } = useUnverifiedFallback(flow.last, onOutcome);
  const { mutation, file } = flow;
  const isDone = outcome !== undefined;
  const isFallback = !isDone && flow.attempts >= MAX_ATTEMPTS;
  const isFirst = flow.attempts === 0 && !isDone;
  return (
    <Paper withBorder p="md" style={{ borderStyle: 'dashed' }}>
      <Stack>
        <Text fw={600}>{t('ticket.title')}</Text>
        {isFirst && file === undefined && (
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
              flow.send(file, true);
            }}
          />
        )}
        <TicketStatus problem={flow.problem} last={flow.last} outcome={outcome} />
        {mutation.isError && <Alert color="red">{t(uploadErrorKey(mutation.error))}</Alert>}
        {isFallback ? (
          <TicketFallback
            isSending={mutation.isPending}
            isDeclaring={declare.isPending}
            isFailed={declare.isError}
            onDisplayPhoto={(photo) => {
              flow.send(photo, false);
            }}
            onReason={onReason}
          />
        ) : (
          <CameraButton
            label={t(isFirst ? 'ticket.capture' : 'ticket.retake')}
            testId="ticket-input"
            variant={isFirst ? 'filled' : 'default'}
            onPick={flow.pick}
          />
        )}
      </Stack>
    </Paper>
  );
}
