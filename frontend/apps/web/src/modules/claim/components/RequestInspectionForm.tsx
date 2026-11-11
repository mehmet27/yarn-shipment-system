import { InspectionKind } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Group, Stack, TextInput } from '@iplik/ui';
import { useState } from 'react';
import { claimErrorKey } from '../claimErrors';
import { useRequestInspection } from '../hooks/useClaims';

const MAX_NOTE_LENGTH = 2000;

/** Kalite laboratuvar analizi ister; not isteğe bağlıdır. */
export function RequestInspectionForm({ claimId }: { claimId: string }) {
  const { t } = useTranslation();
  const [note, setNote] = useState('');
  const mutation = useRequestInspection(claimId);
  const kind = InspectionKind.LAB;
  return (
    <Stack gap="xs">
      <Group align="end">
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
