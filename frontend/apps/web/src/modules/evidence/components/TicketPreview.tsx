import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Stack } from '@iplik/ui';
import { useEffect, useState } from 'react';
import type { QualityIssue } from '../imageQuality';

export type TicketPreviewProps = {
  file: File;
  issues: readonly QualityIssue[];
  isSending: boolean;
  onSend: () => void;
};

function useObjectUrl(file: File): string | undefined {
  const [url, setUrl] = useState<string>();
  useEffect(() => {
    if (typeof URL.createObjectURL !== 'function') return undefined;
    const created = URL.createObjectURL(file);
    setUrl(created);
    return () => {
      URL.revokeObjectURL(created);
    };
  }, [file]);
  return url;
}

/** Çekilen fişin önizlemesi; bulanık/parlak uyarısı gönderimi engellemez, yeniden çekmeyi önerir. */
export function TicketPreview({ file, issues, isSending, onSend }: TicketPreviewProps) {
  const { t } = useTranslation();
  const url = useObjectUrl(file);
  return (
    <Stack>
      {url !== undefined && (
        <img
          src={url}
          alt={t('ticket.previewAlt')}
          style={{ maxWidth: '100%', maxHeight: 320, objectFit: 'contain' }}
        />
      )}
      {issues.map((issue) => (
        <Alert key={issue} color="yellow">
          {t(`ticket.quality.${issue}`)}
        </Alert>
      ))}
      <Button loading={isSending} onClick={onSend}>
        {t('ticket.send')}
      </Button>
    </Stack>
  );
}
