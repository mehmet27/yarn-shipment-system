import type { FireSettlement } from '@iplik/api-client/settlements';
import { useTranslation } from '@iplik/i18n';
import { rateToPct } from '@iplik/units';
import { Alert, Button, Group, Stack, TextInput } from '@iplik/ui';
import { useState } from 'react';
import { useUpdateFireRate } from './hooks';
import type { DraftError } from './settlementDraft';
import { toRateRequest } from './settlementDraft';
import { settlementErrorKey } from './settlementErrors';

/** Fire oranı düzenleme (FIRE_RATE_EDIT): yüzde ve gerekçe; sunucu denetim kaydı tutar. */
export function FireRateForm({ settlement }: { settlement: FireSettlement }) {
  const { t } = useTranslation();
  const initial = (rateToPct(settlement.fireRate) ?? '').replace('.', ',');
  const [ratePct, setRatePct] = useState(initial);
  const [reason, setReason] = useState('');
  const [error, setError] = useState<DraftError>();
  const mutation = useUpdateFireRate(settlement.shipmentId);
  const submit = () => {
    const request = toRateRequest(ratePct, reason);
    setError(typeof request === 'string' ? request : undefined);
    if (typeof request !== 'string') {
      mutation.mutate(request, {
        onSuccess: () => {
          setReason('');
        },
      });
    }
  };
  return (
    <Stack>
      <Group grow align="flex-start">
        <TextInput
          label={t('settlement.fire.ratePct')}
          inputMode="decimal"
          value={ratePct}
          onChange={(event) => {
            setRatePct(event.currentTarget.value);
          }}
        />
        <TextInput
          label={t('settlement.fire.rateReason')}
          value={reason}
          onChange={(event) => {
            setReason(event.currentTarget.value);
          }}
        />
      </Group>
      {error !== undefined && <Alert color="red">{t(`settlement.draftErrors.${error}`)}</Alert>}
      {mutation.isError && <Alert color="red">{t(settlementErrorKey(mutation.error))}</Alert>}
      <Button w="fit-content" loading={mutation.isPending} onClick={submit}>
        {t('settlement.fire.rateSubmit')}
      </Button>
    </Stack>
  );
}
