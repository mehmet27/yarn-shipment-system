import { useTranslation } from '@iplik/i18n';
import { Alert, Autocomplete, SimpleGrid, TextInput } from '@iplik/ui';
import { useProductionRecipe } from '../hooks/useProductionRecipe';
import type { YarnLineDraft } from '../stockDraft';
import type { useProductionSubmit } from '../useProductionSubmit';

type ProductionState = ReturnType<typeof useProductionSubmit>;

type Props = Pick<ProductionState, 'draft' | 'field' | 'setFabricType'> & {
  setYarns: (rows: YarnLineDraft[]) => void;
};

/** Kumaş cinsi (tanımlı karışımlar önerilir, yeni ad da yazılabilir) ve toplam kumaş kilosu; cins iplikleri belirler. */
export function ProductionFabricType({ draft, field, setFabricType, setYarns }: Props) {
  const { t } = useTranslation();
  const recipe = useProductionRecipe(draft.fabricType, { setFabricType, setYarns });
  return (
    <>
      <SimpleGrid cols={{ base: 1, sm: 2 }}>
        <Autocomplete
          label={t('stock.production.fabricType')}
          withAsterisk
          data={recipe.fabricTypes}
          value={draft.fabricType}
          onChange={recipe.change}
          comboboxProps={{ withinPortal: true, hideDetached: false }}
        />
        <TextInput
          label={t('stock.production.netKg')}
          inputMode="decimal"
          withAsterisk
          {...field('netKg')}
        />
      </SimpleGrid>
      {recipe.hasFailed && <Alert color="red">{t('stock.production.fillFailed')}</Alert>}
    </>
  );
}
