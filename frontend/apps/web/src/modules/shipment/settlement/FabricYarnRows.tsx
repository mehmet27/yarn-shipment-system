import { useTranslation } from '@iplik/i18n';
import { Button, Group, Select, Stack, Text, TextInput } from '@iplik/ui';
import type { YarnRowDraft } from './settlementDraft';

export type FabricYarnRowsProps = {
  rows: readonly YarnRowDraft[];
  /** Bu sevkiyattaki iplik adları; satırlarda bunlardan biri seçilir. */
  yarnNames: readonly string[];
  onChange: (index: number, patch: Partial<YarnRowDraft>) => void;
  onAdd: () => void;
  onRemove: (index: number) => void;
};

/** Başka satırda seçilmiş iplik tekrar sunulmaz. */
function optionsFor(names: readonly string[], rows: readonly YarnRowDraft[], index: number) {
  const taken = new Set(rows.filter((_, i) => i !== index).map((row) => row.yarnType));
  return names.filter((name) => !taken.has(name));
}

/** İplik dağılımı satırları; boş bırakılırsa sunucu reçeteyle dağıtır. */
export function FabricYarnRows({
  rows,
  yarnNames,
  onChange,
  onAdd,
  onRemove,
}: FabricYarnRowsProps) {
  const { t } = useTranslation();
  return (
    <Stack gap="xs">
      <Text fw={500}>{t('settlement.fabric.distribution')}</Text>
      <Text size="sm" c="dimmed">
        {t('settlement.fabric.distributionHint')}
      </Text>
      {rows.map((row, index) => (
        // Satırın kimliği yok; yalnız sondan eklenir ve sırayla silinir.
        // eslint-disable-next-line react/no-array-index-key -- kimliksiz taslak satırı
        <Group key={index} grow align="flex-end">
          <Select
            label={t('settlement.fabric.yarnType')}
            placeholder={t('settlement.fabric.yarnPlaceholder')}
            data={optionsFor(yarnNames, rows, index)}
            value={row.yarnType === '' ? null : row.yarnType}
            onChange={(value) => {
              onChange(index, { yarnType: value ?? '' });
            }}
            allowDeselect={false}
            nothingFoundMessage={t('settlement.fabric.noYarnFound')}
          />
          <TextInput
            label={t('settlement.fabric.yarnKg')}
            inputMode="decimal"
            value={row.netKg}
            onChange={(event) => {
              onChange(index, { netKg: event.currentTarget.value });
            }}
          />
          <Button
            variant="default"
            onClick={() => {
              onRemove(index);
            }}
          >
            {t('settlement.fabric.removeYarn')}
          </Button>
        </Group>
      ))}
      <Button variant="default" w="fit-content" onClick={onAdd}>
        {t('settlement.fabric.addYarn')}
      </Button>
    </Stack>
  );
}
