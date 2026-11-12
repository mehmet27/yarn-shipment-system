import type { EvidenceView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Loader, Modal, UnstyledButton, useDisclosure } from '@iplik/ui';
import { useEvidenceUrl } from '../hooks/useEvidenceQueries';

const THUMB_SIZE = 120;

/** Dosya adresi alınamazsa (süre, yetki, depo) küçük bir uyarı gösterilir. */
export function EvidenceImageThumb({ evidence }: { evidence: EvidenceView }) {
  const { t } = useTranslation();
  const [isOpen, { open, close }] = useDisclosure(false);
  const url = useEvidenceUrl(evidence.id);
  const label = t(`evidence.types.${evidence.evidenceType}`);
  if (url.isPending) return <Loader size="sm" />;
  if (url.isError) return <Alert color="red">{t('evidence.view.loadFailed')}</Alert>;
  return (
    <>
      <UnstyledButton onClick={open} aria-label={t('evidence.view.enlarge', { type: label })}>
        <img
          src={url.data.url}
          alt={label}
          style={{ width: THUMB_SIZE, height: THUMB_SIZE, objectFit: 'cover', borderRadius: 4 }}
        />
      </UnstyledButton>
      <Modal opened={isOpen} onClose={close} title={label} size="xl">
        <img src={url.data.url} alt={label} style={{ maxWidth: '100%', maxHeight: '75vh' }} />
      </Modal>
    </>
  );
}
