import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Paper, Stack, Title } from '@iplik/ui';
import { useRefreshDiscrepancies, useResolveDiscrepancy } from '../hooks/useDecideDiscrepancy';
import { ResolutionSelect } from './ResolutionSelect';
import { ResolveActions } from './ResolveActions';
import { ResolveInvoiceFailed } from './ResolveInvoiceFailed';
import { ResolveDraftFields } from './ResolveDraftFields';
import { useResolveChoices } from './useResolveChoices';
import { useResolveDraft } from './useResolveDraft';

const ERROR_KEYS: Record<string, string> = {
  DISCREPANCY_NOT_OPEN: 'discrepancy.resolve.notOpen',
  DISCREPANCY_NOTE_REQUIRED: 'discrepancy.resolve.noteRequired',
  QUALITY_CLAIM_INSPECTION_PENDING: 'discrepancy.resolve.inspectionPending',
};

function errorKey(error: unknown): string {
  return ERROR_KEYS[toProblem(error).code] ?? 'discrepancy.resolve.failed';
}

/**
 * Karar: çözüm ve not zorunlu; sorumluluk atamasında sorumlu taraf, "haklı, faturalandı"da fatura bilgisi
 * de gerekir. Müdür yalnızca haklı/haksız der. Kayıttan önce onay sorulur.
 */
export function ResolveForm({
  discrepancyId,
  shipmentId,
}: {
  discrepancyId: string;
  shipmentId: string;
}) {
  const { t } = useTranslation();
  const { isManager, choices } = useResolveChoices(shipmentId);
  const draft = useResolveDraft();
  const mutation = useResolveDiscrepancy(discrepancyId, shipmentId);
  const refresh = useRefreshDiscrepancies();
  const save = () => {
    const input = draft.toInput();
    if (input === undefined) return;
    mutation.mutate(input, {
      onSettled: () => {
        draft.setConfirming(false);
      },
    });
  };
  if (mutation.data?.isInvoiceFailed === true) {
    return <ResolveInvoiceFailed shipmentId={shipmentId} onDone={() => void refresh()} />;
  }
  return (
    <Paper withBorder p="md" component="section">
      <Stack>
        <Title order={3} size="h4">
          {t('discrepancy.resolve.title')}
        </Title>
        <ResolutionSelect
          choices={choices}
          isManager={isManager}
          value={draft.choice}
          onChange={draft.setChoice}
        />
        <ResolveDraftFields draft={draft} />
        {mutation.isError && <Alert color="red">{t(errorKey(mutation.error))}</Alert>}
        <ResolveActions
          choice={draft.choice}
          invoiceNo={draft.invoice.invoiceNo}
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
