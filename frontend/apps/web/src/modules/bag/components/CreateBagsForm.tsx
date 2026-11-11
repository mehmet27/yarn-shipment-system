import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Group, NumberInput, Stack } from '@iplik/ui';
import { useState } from 'react';
import { useCreateBags } from '../hooks/useCreateBags';
import { bagErrorKey } from './bagErrors';
import { CreatedNotice } from './CreatedNotice';

const MAX_COUNT = 200;
const DEFAULT_COUNT = 10;

export type CreateBagsFormProps = {
  lotId: string;
  onCreated: (uniqueKeys: string[]) => void;
};

/** Seçili partiye istenen sayıda çuval üretir; üretilen anahtarlar baskı için seçilir. */
export function CreateBagsForm({ lotId, onCreated }: CreateBagsFormProps) {
  const { t } = useTranslation();
  const [count, setCount] = useState<number | ''>(DEFAULT_COUNT);
  const mutation = useCreateBags();
  const submit = () => {
    if (count === '') return;
    mutation.mutate(
      { lotId, packageType: 'BAG', count },
      {
        onSuccess: (bags) => {
          onCreated(bags.map((bag) => bag.uniqueKey));
        },
      },
    );
  };
  return (
    <Stack>
      <Group align="flex-end">
        <NumberInput
          w={{ base: '100%', sm: 'auto' }}
          label={t('bag.count')}
          min={1}
          max={MAX_COUNT}
          clampBehavior="strict"
          allowDecimal={false}
          value={count}
          onChange={(value) => {
            setCount(typeof value === 'number' ? value : '');
          }}
        />
        <Button onClick={submit} loading={mutation.isPending} disabled={count === ''}>
          {t('bag.create', { item: t('bag.packageTypes.BAG') })}
        </Button>
      </Group>
      {mutation.isSuccess && (
        <CreatedNotice count={mutation.data.length} packageType={mutation.variables.packageType} />
      )}
      {mutation.isError && (
        <Alert color="red" role="alert">
          {t(bagErrorKey(mutation.error))}
        </Alert>
      )}
    </Stack>
  );
}
