import { Resolution, toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Paper, Select, Stack, TextInput } from '@iplik/ui';
import { useState } from 'react';
import { useResolveDiscrepancy } from '../hooks/useDecideDiscrepancy';

const MAX_NOTE_LENGTH = 2000;
const MAX_PARTY_LENGTH = 200;
const RESOLUTIONS = Object.values(Resolution);

function resolutionOptions(t: (key: string) => string) {
  return RESOLUTIONS.map((value) => ({ value, label: t(`discrepancy.resolution.${value}`) }));
}

function errorKey(error: unknown): string {
  return toProblem(error).code === 'DISCREPANCY_NOT_OPEN'
    ? 'discrepancy.resolve.notOpen'
    : 'discrepancy.resolve.failed';
}

/** Karar: çözüm kodu ve not zorunlu; sorumluluk atamasında sorumlu taraf da gerekir. */
export function ResolveForm({ discrepancyId }: { discrepancyId: string }) {
  const { t } = useTranslation();
  const [resolution, setResolution] = useState<(typeof RESOLUTIONS)[number]>();
  const [note, setNote] = useState('');
  const [liableParty, setLiableParty] = useState('');
  const mutation = useResolveDiscrepancy(discrepancyId);
  const isPartyRequired = resolution === 'LIABILITY_ASSIGNED';
  const canSubmit =
    resolution !== undefined &&
    note.trim() !== '' &&
    (!isPartyRequired || liableParty.trim() !== '');
  return (
    <Paper withBorder p="md">
      <Stack>
        <Select
          label={t('discrepancy.resolve.resolution')}
          data={resolutionOptions(t)}
          value={resolution ?? null}
          onChange={(value) => {
            setResolution(RESOLUTIONS.find((item) => item === value));
          }}
          withAsterisk
        />
        {isPartyRequired && (
          <TextInput
            label={t('discrepancy.resolve.liableParty')}
            maxLength={MAX_PARTY_LENGTH}
            value={liableParty}
            onChange={(event) => {
              setLiableParty(event.currentTarget.value);
            }}
            withAsterisk
          />
        )}
        <TextInput
          label={t('discrepancy.resolve.note')}
          maxLength={MAX_NOTE_LENGTH}
          value={note}
          onChange={(event) => {
            setNote(event.currentTarget.value);
          }}
          withAsterisk
        />
        {mutation.isError && <Alert color="red">{t(errorKey(mutation.error))}</Alert>}
        <Button
          disabled={!canSubmit}
          loading={mutation.isPending}
          onClick={() => {
            if (resolution === undefined) return;
            const party = isPartyRequired ? { liableParty: liableParty.trim() } : {};
            mutation.mutate({ resolution, note: note.trim(), ...party });
          }}
        >
          {t('discrepancy.resolve.submit')}
        </Button>
      </Stack>
    </Paper>
  );
}
