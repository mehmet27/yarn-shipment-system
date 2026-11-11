import { useTranslation } from '@iplik/i18n';
import { Button, Group, Stack, Text, TextInput } from '@iplik/ui';
import type { YarnRowDraft } from './settlementDraft';

export type FabricYarnRowsProps = {
  rows: readonly YarnRowDraft[];
  onChange: (index: number, patch: Partial<YarnRowDraft>) => void;
  onAdd: () => void;
  onRemove: (index: number) => void;
};

/** İplik dağılımı satırları; boş bırakılırsa sunucu reçeteyle dağıtır. */
export function FabricYarnRows({ rows, onChange, onAdd, onRemove }: FabricYarnRowsProps) {
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
          <TextInput
            label={t('settlement.fabric.yarnType')}
            value={row.yarnType}
            onChange={(event) => {
              onChange(index, { yarnType: event.currentTarget.value });
            }}
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
