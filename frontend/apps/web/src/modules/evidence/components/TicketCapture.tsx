import { useTranslation } from '@iplik/i18n';
import { Paper, Stack, Text } from '@iplik/ui';
import { useTicketPhoto } from '../hooks/useTicketPhoto';
import { CameraButton } from './CameraButton';
import { TicketPreview } from './TicketPreview';
import { TicketStatus } from './TicketStatus';

export type TicketCaptureProps = {
  /** Seçilen fotoğraf; işlem onaylanana kadar yalnızca bu cihazda tutulur, sisteme yüklenmez. */
  file: File | undefined;
  onFileChange: (file: File | undefined) => void;
};

/** Kantar fişi çekimi: kamera → önizleme. Yükleme, çıkış tartısı kaydedilirken formun sahibince yapılır. */
export function TicketCapture({ file, onFileChange }: TicketCaptureProps) {
  const { t } = useTranslation();
  const flow = useTicketPhoto(file, onFileChange);
  return (
    <Paper withBorder p="md" style={{ borderStyle: 'dashed' }}>
      <Stack>
        <Text fw={600}>{t('ticket.title')}</Text>
        {file === undefined && (
          <Text size="sm" c="dimmed">
            {t('ticket.guide')}
          </Text>
        )}
        {file !== undefined && <TicketPreview file={file} issues={flow.issues} />}
        <TicketStatus problem={flow.problem} isSelected={file !== undefined} />
        <CameraButton
          label={t(file === undefined ? 'ticket.capture' : 'ticket.retake')}
          testId="ticket-input"
          variant={file === undefined ? 'filled' : 'default'}
          onPick={flow.pick}
        />
      </Stack>
    </Paper>
  );
}
