import { useTranslation } from '@iplik/i18n';
import { Alert, Loader } from '@iplik/ui';
import { useEvidence } from '../hooks/useEvidenceQueries';
import { EvidenceItem } from './EvidenceItem';

/** Yalnızca kimliği bilinen kanıt (lab belgesi, uyuşmazlık etkinliği). */
export function EvidenceById({ evidenceId }: { evidenceId: string }) {
  const { t } = useTranslation();
  const query = useEvidence(evidenceId);
  if (query.isPending) return <Loader size="sm" />;
  if (query.isError) return <Alert color="red">{t('evidence.view.loadFailed')}</Alert>;
  return <EvidenceItem evidence={query.data} />;
}
