import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Stack, Text, TextInput } from '@iplik/ui';
import { formatKg } from '@iplik/units';
import { useState } from 'react';
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
  { name: 'scaleTicketNo', inputMode: 'text' },
  { name: 'sealNo', inputMode: 'text' },
] as const satisfies readonly { name: keyof DispatchDraft; inputMode: string }[];

function failureKey(error: unknown): string {
  return toProblem(error).code === 'SEAL_ALREADY_USED'
    ? 'dispatch.sealAlreadyUsed'
    : 'dispatch.submitFailed';
}

export type DispatchFormProps = { shipmentId: string; onSaved: () => void };

/** Depo çıkış tartısı: kantar fişi ve mühür tek formda; net yalnızca brüt ve daradan hesaplanır. */
export function DispatchForm({ shipmentId, onSaved }: DispatchFormProps) {
  const { t } = useTranslation();
  const [draft, setDraft] = useState<DispatchDraft>(EMPTY_DISPATCH_DRAFT);
  const [error, setError] = useState<DispatchError>();
  const mutation = useRecordDispatch(shipmentId);
  const weighing = parseDispatchWeighing(draft);
  const submit = () => {
    const validation = validateDispatch(draft);
    setError(validation);
    const request = toDispatchRequest(draft);
    if (request !== undefined) mutation.mutate(request, { onSuccess: onSaved });
  };
  return (
    <Stack maw={480}>
      <Text size="sm" c="dimmed">
        {t('dispatch.hint')}
      </Text>
      {FIELDS.map(({ name, inputMode }) => (
        <TextInput
          key={name}
          inputMode={inputMode}
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
      {error !== undefined && <Alert color="red">{t(`dispatch.errors.${error}`)}</Alert>}
      {mutation.isError && <Alert color="red">{t(failureKey(mutation.error))}</Alert>}
      <Button loading={mutation.isPending} onClick={submit}>
        {t('dispatch.submit')}
      </Button>
    </Stack>
  );
}
