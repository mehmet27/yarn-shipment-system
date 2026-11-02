import type { PackageType } from '@iplik/api-client';
import { PackageType as PackageTypes } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Group, NumberInput, Select, Stack } from '@iplik/ui';
import { useState } from 'react';
import { useCreateBags } from '../hooks/useCreateBags';
import { bagErrorKey } from './bagErrors';

const PACKAGE_TYPES = Object.values(PackageTypes);
const MAX_COUNT = 200;
const DEFAULT_COUNT = 10;

export type CreateBagsFormProps = {
  lotId: string;
  onCreated: (uniqueKeys: string[]) => void;
};

/** Seçili partiye istenen sayıda çuval üretir; üretilen anahtarlar baskı için seçilir. */
export function CreateBagsForm({ lotId, onCreated }: CreateBagsFormProps) {
  const { t } = useTranslation();
  const [packageType, setPackageType] = useState<PackageType>('BAG');
  const [count, setCount] = useState<number | ''>(DEFAULT_COUNT);
  const mutation = useCreateBags();
  const submit = () => {
    if (count === '') return;
    mutation.mutate(
      { lotId, packageType, count },
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
        <Select
          label={t('bag.packageType')}
          data={PACKAGE_TYPES.map((value) => ({ value, label: t(`bag.packageTypes.${value}`) }))}
          value={packageType}
          onChange={(value) => {
            setPackageType(PACKAGE_TYPES.find((item) => item === value) ?? 'BAG');
          }}
          allowDeselect={false}
        />
        <NumberInput
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
          {t('bag.create')}
        </Button>
      </Group>
      {mutation.isSuccess && (
        <Alert color="green" role="status">
          {t('bag.created', { count: mutation.data.length })}
        </Alert>
      )}
      {mutation.isError && <Alert color="red">{t(bagErrorKey(mutation.error))}</Alert>}
    </Stack>
  );
}
