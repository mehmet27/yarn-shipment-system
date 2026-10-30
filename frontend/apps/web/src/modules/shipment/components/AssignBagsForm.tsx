import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Group, Stack, TextInput } from '@iplik/ui';
import { useState } from 'react';
import { addBagKey } from '../delivery/deliveryDraft';
import { useAssignBags } from '../hooks/useAssignBags';
import { assignErrorKey } from './assignErrors';
import { PendingKeys } from './PendingKeys';

/** Depo: çuval anahtarını okutur/yazar, listeden çıkarabilir, sonra hepsini tek istekle atar. */
export function AssignBagsForm({ shipmentId }: { shipmentId: string }) {
  const { t } = useTranslation();
  const [input, setInput] = useState('');
  const [keys, setKeys] = useState<readonly string[]>([]);
  const mutation = useAssignBags(shipmentId);
  const add = () => {
    setKeys(addBagKey(keys, input));
    setInput('');
  };
  const assign = () => {
    mutation.mutate(
      { uniqueKeys: [...keys], scanMethod: 'MANUAL' },
      {
        onSuccess: () => {
          setKeys([]);
        },
      },
    );
  };
  return (
    <Stack>
      <Group align="flex-end" wrap="nowrap">
        <TextInput
          style={{ flex: 1 }}
          label={t('shipment.bags.assign.key')}
          value={input}
          onChange={(event) => {
            setInput(event.currentTarget.value);
          }}
          onKeyDown={(event) => {
            if (event.key === 'Enter') add();
          }}
        />
        <Button variant="default" onClick={add}>
          {t('shipment.bags.assign.add')}
        </Button>
      </Group>
      <PendingKeys
        keys={keys}
        onRemove={(key) => {
          setKeys(keys.filter((item) => item !== key));
        }}
      />
      {mutation.isError && (
        <Alert color="red">{t(assignErrorKey(toProblem(mutation.error).code))}</Alert>
      )}
      {mutation.isSuccess &&
        (mutation.data.assignedCount === 0 ? (
          <Alert color="yellow">{t('shipment.bags.assign.nothingNew')}</Alert>
        ) : (
          <Alert color="green">
            {t('shipment.bags.assign.done', { count: mutation.data.assignedCount })}
          </Alert>
        ))}
      <Button disabled={keys.length === 0} loading={mutation.isPending} onClick={assign}>
        {t('shipment.bags.assign.submit', { count: keys.length })}
      </Button>
    </Stack>
  );
}
