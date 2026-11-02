import type { CorrectionTarget, InternalShipmentView } from '@iplik/api-client';
import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Group, Paper, Select, Stack, TextInput } from '@iplik/ui';
import { useState } from 'react';
import { useRecordCorrection } from '../hooks/useCorrections';
import type { CorrectionDraft, CorrectionError } from './correctionDraft';
import {
  correctableTargets,
  EMPTY_CORRECTION_DRAFT,
  isWeighingTarget,
  MAX_REASON_LENGTH,
  toCorrectionRequest,
} from './correctionDraft';

const SERVER_CODES = ['INVALID_CORRECTION', 'SEAL_ALREADY_USED', 'CORRECTION_CONFLICT'] as const;

function serverErrorKey(error: unknown): string {
  const code = SERVER_CODES.find((candidate) => candidate === toProblem(error).code);
  return `shipment.correction.errors.${code ?? 'unknown'}`;
}

/** API kilosu ("2000.500") Türkçe girdi biçimine ("2000,5"). */
const toInputKg = (kg: string) => String(Number(kg)).replace('.', ',');

/** Seçilen tartımın güncel değerleri forma yazılır; Admin yalnızca değişeni düzeltir. */
function draftFor(shipment: InternalShipmentView, target: CorrectionTarget): CorrectionDraft {
  const weighing =
    target === 'DISPATCH_WEIGHING' ? shipment.dispatchWeighing : shipment.deliveryWeighing;
  return {
    ...EMPTY_CORRECTION_DRAFT,
    target,
    grossKg: isWeighingTarget(target) && weighing !== undefined ? toInputKg(weighing.grossKg) : '',
    tareKg: isWeighingTarget(target) && weighing !== undefined ? toInputKg(weighing.tareKg) : '',
    sealNo: target === 'DISPATCH_SEAL_NO' ? (shipment.dispatchSealNo ?? '') : '',
  };
}

/** Taslak alanları; hedef değişince seçilen kaydın güncel değeri yazılır. */
function useCorrectionDraft(shipment: InternalShipmentView) {
  const [draft, setDraft] = useState<CorrectionDraft>(EMPTY_CORRECTION_DRAFT);
  const text = (name: 'grossKg' | 'tareKg' | 'sealNo' | 'reason') => ({
    value: draft[name],
    onChange: (event: React.ChangeEvent<HTMLInputElement>) => {
      const { value } = event.currentTarget;
      setDraft((current) => ({ ...current, [name]: value }));
    },
  });
  const selectTarget = (target: CorrectionTarget | undefined) => {
    setDraft(target === undefined ? EMPTY_CORRECTION_DRAFT : draftFor(shipment, target));
  };
  const reset = () => {
    setDraft(EMPTY_CORRECTION_DRAFT);
  };
  return { draft, text, selectTarget, reset };
}

/** Admin: kilitli sevkiyatta da tartım ya da mühür numarasını gerekçeyle düzeltir; eski değer saklanır. */
export function CorrectionForm({ shipment }: { shipment: InternalShipmentView }) {
  const { t } = useTranslation();
  const { draft, text, selectTarget, reset } = useCorrectionDraft(shipment);
  const [error, setError] = useState<CorrectionError>();
  const mutation = useRecordCorrection(shipment.shipmentId);
  const targets = correctableTargets(shipment);
  const submit = () => {
    const request = toCorrectionRequest(draft);
    setError(typeof request === 'string' ? request : undefined);
    if (typeof request !== 'string') mutation.mutate(request, { onSuccess: reset });
  };
  return (
    <Paper withBorder p="md">
      <Stack>
        <Select
          label={t('shipment.correction.target')}
          data={targets.map((value) => ({
            value,
            label: t(`shipment.correction.targets.${value}`),
          }))}
          value={draft.target ?? null}
          onChange={(value) => {
            selectTarget(targets.find((item) => item === value));
          }}
          withAsterisk
        />
        {isWeighingTarget(draft.target) && (
          <Group grow>
            <TextInput label={t('shipment.file.gross')} inputMode="decimal" {...text('grossKg')} />
            <TextInput label={t('shipment.file.tare')} inputMode="decimal" {...text('tareKg')} />
          </Group>
        )}
        {draft.target !== undefined && !isWeighingTarget(draft.target) && (
          <TextInput label={t('shipment.correction.sealNo')} maxLength={64} {...text('sealNo')} />
        )}
        <TextInput
          label={t('shipment.correction.reason')}
          maxLength={MAX_REASON_LENGTH}
          withAsterisk
          {...text('reason')}
        />
        {error !== undefined && (
          <Alert color="red">{t(`shipment.correction.errors.${error}`)}</Alert>
        )}
        {mutation.isError && <Alert color="red">{t(serverErrorKey(mutation.error))}</Alert>}
        <Button w="fit-content" loading={mutation.isPending} onClick={submit}>
          {t('shipment.correction.submit')}
        </Button>
      </Stack>
    </Paper>
  );
}
