import type { Resolution } from '@iplik/api-client';
import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Paper, Stack, Title } from '@iplik/ui';
import { useState } from 'react';
import { useResolveDiscrepancy } from '../hooks/useDecideDiscrepancy';
import { ResolutionSelect } from './ResolutionSelect';
import { ResolveActions } from './ResolveActions';
import { ResolveNoteFields } from './ResolveNoteFields';

const ERROR_KEYS: Record<string, string> = {
  DISCREPANCY_NOT_OPEN: 'discrepancy.resolve.notOpen',
  DISCREPANCY_NOTE_REQUIRED: 'discrepancy.resolve.noteRequired',
  QUALITY_CLAIM_INSPECTION_PENDING: 'discrepancy.resolve.inspectionPending',
};

function errorKey(error: unknown): string {
  return ERROR_KEYS[toProblem(error).code] ?? 'discrepancy.resolve.failed';
}

const partyOf = (isRequired: boolean, liableParty: string) =>
  isRequired ? { liableParty: liableParty.trim() } : {};

/** Karar: çözüm kodu ve not zorunlu; sorumluluk atamasında sorumlu taraf da gerekir. Kayıttan önce onay sorulur. */
export function ResolveForm({ discrepancyId }: { discrepancyId: string }) {
  const { t } = useTranslation();
  const [resolution, setResolution] = useState<Resolution>();
  const [note, setNote] = useState('');
  const [liableParty, setLiableParty] = useState('');
  const [isConfirming, setConfirming] = useState(false);
  const mutation = useResolveDiscrepancy(discrepancyId);
  const isPartyRequired = resolution === 'LIABILITY_ASSIGNED';
  const canSubmit =
    resolution !== undefined &&
    note.trim() !== '' &&
    (!isPartyRequired || liableParty.trim() !== '');
  const save = () => {
    if (resolution === undefined) return;
    const request = { resolution, note: note.trim(), ...partyOf(isPartyRequired, liableParty) };
    mutation.mutate(request, {
      onSettled: () => {
        setConfirming(false);
      },
    });
  };
  return (
    <Paper withBorder p="md" component="section">
      <Stack>
        <Title order={3} size="h4">
          {t('discrepancy.resolve.title')}
        </Title>
        <ResolutionSelect
          value={resolution}
          onChange={(value) => {
            setResolution(value);
            setConfirming(false);
          }}
        />
        <ResolveNoteFields
          {...{ isPartyRequired, liableParty, note }}
          onLiableParty={setLiableParty}
          onNote={setNote}
        />
        {mutation.isError && <Alert color="red">{t(errorKey(mutation.error))}</Alert>}
        <ResolveActions
          resolution={resolution}
          isConfirming={isConfirming}
          canSubmit={canSubmit}
          isPending={mutation.isPending}
          onAsk={() => {
            mutation.reset();
            setConfirming(true);
          }}
          onConfirm={save}
          onCancel={() => {
            setConfirming(false);
          }}
        />
      </Stack>
    </Paper>
  );
}
