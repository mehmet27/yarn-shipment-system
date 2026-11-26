import { useTranslation } from '@iplik/i18n';
import { Button, SimpleGrid, Stack, Text, TextInput } from '@iplik/ui';
import { MasterSelect } from '@/modules/shipment';
import type { ProductionDraft, YarnLineDraft } from '../stockDraft';
import { ProductionShareNote } from './ProductionShareNote';

export type ProductionYarnRowsProps = {
  draft: ProductionDraft;
  onChange: (index: number, patch: Partial<YarnLineDraft>) => void;
  onAdd: () => void;
  onRemove: (index: number) => void;
};

/** Kumaşın karışımı; her satırda iplik cinsi, yüzdesi ve stoktan ne kadar düşeceği. */
export function ProductionYarnRows({ draft, onChange, onAdd, onRemove }: ProductionYarnRowsProps) {
  const { t } = useTranslation();
  const rows = draft.yarns;
  return (
    <Stack gap="xs">
      <Text fw={500}>{t('stock.production.yarns')}</Text>
      {rows.map((row, index) => (
        // Satırın kimliği yok; yalnız sondan eklenir ve sırayla silinir.
        // eslint-disable-next-line react/no-array-index-key -- kimliksiz taslak satırı
        <Stack key={index} gap={4}>
          <SimpleGrid cols={{ base: 1, sm: 3 }} style={{ alignItems: 'end' }}>
            <MasterSelect
              kind="product"
              label={`${t('stock.production.yarnType')} ${String(index + 1)}`}
              value={row.product}
              onChange={(product) => {
                onChange(index, { product });
              }}
            />
            <TextInput
              label={`${t('stock.production.ratio')} ${String(index + 1)}`}
              inputMode="decimal"
              value={row.ratio}
              onChange={(event) => {
                onChange(index, { ratio: event.currentTarget.value });
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
          </SimpleGrid>
          <ProductionShareNote draft={draft} row={row} />
        </Stack>
      ))}
      <Button variant="default" w="fit-content" onClick={onAdd}>
        {t('stock.production.addYarn')}
      </Button>
    </Stack>
  );
}
