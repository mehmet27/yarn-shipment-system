import { useTranslation } from '@iplik/i18n';
import { Button, Group, TextInput } from '@iplik/ui';

/** Çuval kilosu girişi; Enter ya da düğme kiloyu listeye ekler. */
export function WeightEntry({
  value,
  isInvalid,
  onChange,
  onAdd,
}: {
  value: string;
  isInvalid: boolean;
  onChange: (value: string) => void;
  onAdd: () => void;
}) {
  const { t } = useTranslation();
  return (
    <Group align="flex-end" wrap="nowrap">
      <TextInput
        style={{ flex: 1 }}
        label={t('shipment.bags.assign.weight')}
        value={value}
        error={isInvalid ? t('shipment.bags.assign.invalidKg') : undefined}
        onChange={(event) => {
          onChange(event.currentTarget.value);
        }}
        onKeyDown={(event) => {
          if (event.key === 'Enter') onAdd();
        }}
      />
      <Button variant="default" onClick={onAdd}>
        {t('shipment.bags.assign.add')}
      </Button>
    </Group>
  );
}
