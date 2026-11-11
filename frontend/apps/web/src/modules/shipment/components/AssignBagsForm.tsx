import { useTranslation } from '@iplik/i18n';
import { parseKg } from '@iplik/units';
import { Button, Stack } from '@iplik/ui';
import { useState } from 'react';
import { useAssignBags } from '../hooks/useAssignBags';
import { AssignResult } from './AssignResult';
import { WeightEntry } from './WeightEntry';
import { PendingWeights } from './PendingWeights';

/** Tek istekte en fazla bu kadar çuval gönderilir (sözleşme sınırı). */
const MAX_BAGS = 500;

/** Depo: çuvalların kilosunu yazar; numaralar girilme sırasıyla verilir, hepsi tek istekle eklenir. */
export function AssignBagsForm({
  shipmentId,
  assignedCount,
}: {
  shipmentId: string;
  assignedCount: number;
}) {
  const { t } = useTranslation();
  const [input, setInput] = useState('');
  const [weights, setWeights] = useState<readonly string[]>([]);
  const [isInvalid, setIsInvalid] = useState(false);
  const mutation = useAssignBags(shipmentId);
  const add = () => {
    if (input.trim() === '' || weights.length >= MAX_BAGS) return;
    const kg = parseKg(input);
    if (kg === undefined || Number(kg) <= 0) {
      setIsInvalid(true);
      return;
    }
    setIsInvalid(false);
    setWeights([...weights, kg]);
    setInput('');
  };
  const submit = () => {
    mutation.mutate(
      { weightsKg: [...weights] },
      {
        onSuccess: () => {
          setWeights([]);
        },
      },
    );
  };
  return (
    <Stack>
      <WeightEntry value={input} isInvalid={isInvalid} onChange={setInput} onAdd={add} />
      <PendingWeights
        weights={weights}
        firstSeq={assignedCount + 1}
        onRemove={(index) => {
          setWeights(weights.filter((_, position) => position !== index));
        }}
      />
      <AssignResult error={mutation.error} count={mutation.data?.assignedCount} />
      <Button disabled={weights.length === 0} loading={mutation.isPending} onClick={submit}>
        {t('shipment.bags.assign.submit', { count: weights.length })}
      </Button>
    </Stack>
  );
}
