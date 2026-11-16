import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Stack, Text, TextInput } from '@iplik/ui';
import { formatKg } from '@iplik/units';
import { useState } from 'react';
import { TicketCapture } from '@/modules/evidence';
import { useRecordDispatch } from '../hooks/useRecordDispatch';
import { PhotoFailedNotice } from './PhotoFailedNotice';
import type { DispatchDraft, DispatchError } from './dispatchDraft';
import {
  EMPTY_DISPATCH_DRAFT,
  parseDispatchWeighing,
  toDispatchRequest,
  validateDispatch,
} from './dispatchDraft';

const FIELDS = [
  // Metin "önce boş araç (dara), sonra dolu araç (brüt)" der; alanlar da bu sırayla gelir.
  { name: 'tareKg', inputMode: 'decimal' },
  { name: 'grossKg', inputMode: 'decimal' },
  { name: 'bagCount', inputMode: 'numeric' },
  { name: 'shortfallReason', inputMode: 'text' },
] as const satisfies readonly { name: Exclude<keyof DispatchDraft, 'ticket'>; inputMode: string }[];

function failureKey(error: unknown): string {
  const { code } = toProblem(error);
  if (code === 'INVALID_SHIPMENT_STATUS') return 'dispatch.notDraft';
  if (code === 'SHIPMENT_NOT_DUE') return 'dispatch.notDue';
  if (code === 'INVALID_DISPATCH') return 'dispatch.inconsistent';
  return 'dispatch.submitFailed';
}

type DispatchMutation = ReturnType<typeof useRecordDispatch>;

/** Geçerli taslağı gönderir; fotoğraf yüklendi mi bilgisini `done` ile bildirir. */
function sendDispatch(
  draft: DispatchDraft,
  mutation: DispatchMutation,
  done: (isPhotoUploaded: boolean) => void,
) {
  const request = toDispatchRequest(draft);
  if (request === undefined || draft.ticket === undefined) return;
  mutation.mutate(
    { request, photo: draft.ticket },
    {
      onSuccess: (result) => {
        done(result.photoUploaded);
      },
    },
  );
}

export type DispatchFormProps = { shipmentId: string; onSaved: () => void };

/** Depo çıkış tartısı: kantar fişi tek formda; net yalnızca brüt ve daradan hesaplanır. */
export function DispatchForm({ shipmentId, onSaved }: DispatchFormProps) {
  const { t } = useTranslation();
  const [draft, setDraft] = useState<DispatchDraft>(EMPTY_DISPATCH_DRAFT);
  const [error, setError] = useState<DispatchError>();
  const [isPhotoMissing, setPhotoMissing] = useState(false);
  const mutation = useRecordDispatch(shipmentId);
  const weighing = parseDispatchWeighing(draft);
  const submit = () => {
    setError(validateDispatch(draft));
    sendDispatch(draft, mutation, (isUploaded) => {
      if (isUploaded) onSaved();
      else setPhotoMissing(true);
    });
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
          {...(name === 'grossKg' || name === 'tareKg' ? { rightSection: 'kg' } : {})}
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
          file={draft.ticket}
          onFileChange={(ticket) => {
            setDraft({ ...draft, ticket });
          }}
        />
      )}
      {error !== undefined && <Alert color="red">{t(`dispatch.errors.${error}`)}</Alert>}
      {mutation.isError && <Alert color="red">{t(failureKey(mutation.error))}</Alert>}
      {isPhotoMissing && <PhotoFailedNotice onContinue={onSaved} />}
      {!isPhotoMissing && (
        <Button loading={mutation.isPending} onClick={submit}>
          {t('dispatch.submit')}
        </Button>
      )}
    </Stack>
  );
}
