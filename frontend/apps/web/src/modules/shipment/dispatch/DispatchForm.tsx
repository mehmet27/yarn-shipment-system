import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Stack, Text, TextInput } from '@iplik/ui';
import { formatKg } from '@iplik/units';
import { useState } from 'react';
import { TicketCapture } from '@/modules/evidence';
import { useRecordDispatch } from '../hooks/useRecordDispatch';
import type { DispatchDraft, DispatchError } from './dispatchDraft';
import {
  EMPTY_DISPATCH_DRAFT,
  parseDispatchWeighing,
  toDispatchRequest,
  validateDispatch,
} from './dispatchDraft';

const FIELDS = [
  { name: 'grossKg', inputMode: 'decimal' },
  { name: 'tareKg', inputMode: 'decimal' },
  { name: 'bagCount', inputMode: 'numeric' },
  { name: 'shortfallReason', inputMode: 'text' },
] as const satisfies readonly { name: Exclude<keyof DispatchDraft, 'ticket'>; inputMode: string }[];

function failureKey(error: unknown): string {
  const { code } = toProblem(error);
  if (code === 'INVALID_SHIPMENT_STATUS') return 'dispatch.notDraft';
  if (code === 'INVALID_DISPATCH') return 'dispatch.inconsistent';
  return 'dispatch.submitFailed';
}

export type DispatchFormProps = { shipmentId: string; onSaved: () => void };

/** Depo çıkış tartısı: kantar fişi tek formda; net yalnızca brüt ve daradan hesaplanır. */
export function DispatchForm({ shipmentId, onSaved }: DispatchFormProps) {
  const { t } = useTranslation();
  const [draft, setDraft] = useState<DispatchDraft>(EMPTY_DISPATCH_DRAFT);
  const [isTicketPending, setIsTicketPending] = useState(false);
  const [error, setError] = useState<DispatchError>();
  const mutation = useRecordDispatch(shipmentId);
  const weighing = parseDispatchWeighing(draft);
  const submit = () => {
    const validation = validateDispatch(draft, isTicketPending);
    setError(validation);
    const request = toDispatchRequest(draft, isTicketPending);
    if (request !== undefined) mutation.mutate(request, { onSuccess: onSaved });
  };
  return (
    <Stack>
      <Text size="sm" c="dimmed">
        {t('dispatch.hint')}
      </Text>
      <Text size="sm" c="dimmed">
        {t('dispatch.mismatchNotice')}
      </Text>
      {FIELDS.map(({ name, inputMode }) => (
        <TextInput
          key={name}
          inputMode={inputMode}
          {...(name === 'shortfallReason' ? { maxLength: 500 } : {})}
          label={t(`dispatch.${name}`)}
          value={draft[name]}
          onChange={(event) => {
            setDraft({ ...draft, [name]: event.currentTarget.value });
          }}
        />
      ))}
      {typeof weighing !== 'string' && (
        <Text fw={700}>{t('dispatch.net', { net: formatKg(weighing.netKg) })}</Text>
      )}
      {typeof weighing !== 'string' && (
        <TicketCapture
          shipmentId={shipmentId}
          evidenceType="DISPATCH_SCALE"
          outcome={draft.ticket}
          onPendingChange={setIsTicketPending}
          onOutcome={(ticket) => {
            setDraft({ ...draft, ticket });
          }}
        />
      )}
      {error !== undefined && <Alert color="red">{t(`dispatch.errors.${error}`)}</Alert>}
      {mutation.isError && <Alert color="red">{t(failureKey(mutation.error))}</Alert>}
      <Button loading={mutation.isPending} onClick={submit}>
        {t('dispatch.submit')}
      </Button>
    </Stack>
  );
}
