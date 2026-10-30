import type { InspectionKind as Kind } from '@iplik/api-client';
import { InspectionKind } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Group, SegmentedControl, Stack, TextInput } from '@iplik/ui';
import { useState } from 'react';
import { claimErrorKey } from '../claimErrors';
import { useRequestInspection } from '../hooks/useClaims';

const MAX_NOTE_LENGTH = 2000;
const KINDS = Object.values(InspectionKind);

/** Kalite numune kontrolü ya da laboratuvar analizi ister; not isteğe bağlıdır. */
export function RequestInspectionForm({ claimId }: { claimId: string }) {
  const { t } = useTranslation();
  const [kind, setKind] = useState<Kind>('SAMPLE');
  const [note, setNote] = useState('');
  const mutation = useRequestInspection(claimId);
  return (
    <Stack gap="xs">
      <Group align="end">
        <SegmentedControl
          aria-label={t('claim.inspection.kindLabel')}
          data={KINDS.map((value) => ({ value, label: t(`claim.inspection.kind.${value}`) }))}
          value={kind}
          onChange={(value) => {
            setKind(KINDS.find((item) => item === value) ?? 'SAMPLE');
          }}
        />
        <TextInput
          label={t('claim.inspection.note')}
          maxLength={MAX_NOTE_LENGTH}
          value={note}
          onChange={(event) => {
            setNote(event.currentTarget.value);
          }}
        />
        <Button
          variant="light"
          loading={mutation.isPending}
          onClick={() => {
            const trimmed = note.trim();
            mutation.mutate(trimmed === '' ? { kind } : { kind, note: trimmed });
          }}
        >
          {t('claim.inspection.request')}
        </Button>
      </Group>
      {mutation.isError && <Alert color="red">{t(claimErrorKey(mutation.error))}</Alert>}
    </Stack>
  );
}
