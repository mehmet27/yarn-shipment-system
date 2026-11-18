import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Paper, Stack, Title } from '@iplik/ui';
import { useResolveDiscrepancy } from '../hooks/useDecideDiscrepancy';
import { RESOLUTIONS } from './resolveChoices';
import { ResolutionSelect } from './ResolutionSelect';
import { ResolveActions } from './ResolveActions';
import { ResolveDraftFields } from './ResolveDraftFields';
import { useResolveDraft } from './useResolveDraft';

const ERROR_KEYS: Record<string, string> = {
  DISCREPANCY_NOT_OPEN: 'discrepancy.resolve.notOpen',
  DISCREPANCY_NOTE_REQUIRED: 'discrepancy.resolve.noteRequired',
  LIABLE_PARTY_REQUIRED: 'discrepancy.resolve.liablePartyRequired',
  QUALITY_CLAIM_INSPECTION_PENDING: 'discrepancy.resolve.inspectionPending',
};

function errorKey(error: unknown): string {
  return ERROR_KEYS[toProblem(error).code] ?? 'discrepancy.resolve.failed';
}

/**
 * Karar: çözüm ve not zorunlu; sorumluluk atamasında sorumlu taraf da gerekir. Müdür yalnızca haklı/haksız der.
 * Haklı karar faturalanacak kiloya eklenir; fatura Faturalanacaklar'dan girilir. Kayıttan önce onay sorulur.
 */
export function ResolveForm({
  discrepancyId,
  shipmentId,
}: {
  discrepancyId: string;
  shipmentId: string;
}) {
  const { t } = useTranslation();
  const draft = useResolveDraft();
  const mutation = useResolveDiscrepancy(discrepancyId, shipmentId);
  const save = () => {
    const request = draft.toRequest();
    if (request === undefined) return;
    mutation.mutate(request, {
      onSettled: () => {
        draft.setConfirming(false);
      },
    });
  };
  return (
    <Paper withBorder p="md" component="section">
      <Stack>
        <Title order={3} size="h4">
          {t('discrepancy.resolve.title')}
        </Title>
        <ResolutionSelect choices={RESOLUTIONS} value={draft.choice} onChange={draft.setChoice} />
        <ResolveDraftFields draft={draft} />
        {mutation.isError && <Alert color="red">{t(errorKey(mutation.error))}</Alert>}
        <ResolveActions
          choice={draft.choice}
          isConfirming={draft.isConfirming}
          canSubmit={draft.canSubmit}
          isPending={mutation.isPending}
          onAsk={() => {
            mutation.reset();
            draft.setConfirming(true);
          }}
          onConfirm={save}
          onCancel={() => {
            draft.setConfirming(false);
          }}
        />
      </Stack>
    </Paper>
  );
}
