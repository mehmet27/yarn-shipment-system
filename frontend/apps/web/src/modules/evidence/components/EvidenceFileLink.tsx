import type { EvidenceView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Anchor } from '@iplik/ui';
import { useEvidenceUrl } from '../hooks/useEvidenceQueries';

const PDF_TYPE = 'application/pdf';

export function EvidenceFileLink({ evidence }: { evidence: EvidenceView }) {
  const { t } = useTranslation();
  const url = useEvidenceUrl(evidence.id);
  if (url.isError) return <Alert color="red">{t('evidence.view.loadFailed')}</Alert>;
  return (
    <Anchor href={url.data?.url} target="_blank" rel="noopener noreferrer">
      {evidence.mimeType === PDF_TYPE ? t('evidence.view.openPdf') : t('evidence.view.openFile')}
    </Anchor>
  );
}
