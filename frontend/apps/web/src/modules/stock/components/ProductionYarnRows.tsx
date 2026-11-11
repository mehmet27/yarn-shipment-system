import { useTranslation } from '@iplik/i18n';
import { Button, Group, Stack, Text, TextInput } from '@iplik/ui';
import type { YarnLineDraft } from '../stockDraft';

export type ProductionYarnRowsProps = {
  rows: readonly YarnLineDraft[];
  onChange: (index: number, patch: Partial<YarnLineDraft>) => void;
  onAdd: () => void;
  onRemove: (index: number) => void;
};

/** Kumaşın yapıldığı iplik satırları; her satırda iplik cinsi ve ondan çıkan kumaş kilosu. */
export function ProductionYarnRows({ rows, onChange, onAdd, onRemove }: ProductionYarnRowsProps) {
  const { t } = useTranslation();
  return (
    <Stack gap="xs">
      <Text fw={500}>{t('stock.production.yarns')}</Text>
      {rows.map((row, index) => (
        // Satırın kimliği yok; yalnız sondan eklenir ve sırayla silinir.
        // eslint-disable-next-line react/no-array-index-key -- kimliksiz taslak satırı
        <Group key={index} grow align="flex-end">
          <TextInput
            label={`${t('stock.production.yarnType')} ${String(index + 1)}`}
            value={row.yarnType}
            onChange={(event) => {
              onChange(index, { yarnType: event.currentTarget.value });
            }}
          />
          <TextInput
            label={`${t('stock.production.yarnKg')} ${String(index + 1)}`}
            inputMode="decimal"
            value={row.netKg}
            onChange={(event) => {
              onChange(index, { netKg: event.currentTarget.value });
            }}
          />
          <Button
            variant="default"
            disabled={rows.length === 1}
            onClick={() => {
              onRemove(index);
            }}
          >
            {t('stock.production.removeYarn')}
          </Button>
        </Group>
      ))}
      <Button variant="default" w="fit-content" onClick={onAdd}>
        {t('stock.production.addYarn')}
      </Button>
    </Stack>
  );
}
