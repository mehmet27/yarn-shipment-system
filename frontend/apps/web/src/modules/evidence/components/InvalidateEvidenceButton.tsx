import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Modal, Stack, Textarea, useDisclosure } from '@iplik/ui';
import { useState } from 'react';
import { Can } from '@/modules/auth';
import { useInvalidateEvidence } from '../hooks/useInvalidateEvidence';

const MAX_REASON_LENGTH = 500;
const KNOWN_CODES = ['EVIDENCE_ALREADY_INVALIDATED', 'INVALIDATION_REASON_REQUIRED'];

function errorKey(error: unknown): string {
  const { code } = toProblem(error);
  return `evidence.invalidate.errors.${KNOWN_CODES.includes(code) ? code : 'unknown'}`;
}

/** Yalnızca `EVIDENCE_INVALIDATE` izni olana görünür; gerekçe zorunlu, dosya silinmez. */
export function InvalidateEvidenceButton({ evidenceId }: { evidenceId: string }) {
  const { t } = useTranslation();
  const [isOpen, { open, close }] = useDisclosure(false);
  const [reason, setReason] = useState('');
  const mutation = useInvalidateEvidence(evidenceId);
  const trimmed = reason.trim();
  return (
    <Can permission="EVIDENCE_INVALIDATE">
      <Button size="compact-xs" color="red" variant="light" onClick={open}>
        {t('evidence.invalidate.button')}
      </Button>
      <Modal opened={isOpen} onClose={close} title={t('evidence.invalidate.title')}>
        <Stack>
          <Alert color="yellow">{t('evidence.invalidate.warning')}</Alert>
          <Textarea
            label={t('evidence.invalidate.reason')}
            withAsterisk
            maxLength={MAX_REASON_LENGTH}
            value={reason}
            onChange={(event) => {
              setReason(event.currentTarget.value);
            }}
          />
          {mutation.isError && <Alert color="red">{t(errorKey(mutation.error))}</Alert>}
          <Button
            color="red"
            loading={mutation.isPending}
            disabled={trimmed === ''}
            onClick={() => {
              mutation.mutate(trimmed, { onSuccess: close });
            }}
          >
            {t('evidence.invalidate.confirm')}
          </Button>
        </Stack>
      </Modal>
    </Can>
  );
}
