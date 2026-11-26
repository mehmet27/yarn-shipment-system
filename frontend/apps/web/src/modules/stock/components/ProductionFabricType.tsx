import { useTranslation } from '@iplik/i18n';
import { Alert, Autocomplete, Button, SimpleGrid, TextInput } from '@iplik/ui';
import { useProductionRecipe } from '../hooks/useProductionRecipe';
import type { YarnLineDraft } from '../stockDraft';
import type { useProductionSubmit } from '../useProductionSubmit';

type ProductionState = ReturnType<typeof useProductionSubmit>;

type Props = Pick<ProductionState, 'draft' | 'field' | 'setFabricType'> & {
  setYarns: (rows: YarnLineDraft[]) => void;
};

/** Kumaş cinsi (tanımlı karışımlar önerilir, yeni ad da yazılabilir), toplam kumaş kilosu ve karışımı yazma düğmesi. */
export function ProductionFabricType({ draft, field, setFabricType, setYarns }: Props) {
  const { t } = useTranslation();
  const recipe = useProductionRecipe(draft.fabricType, setYarns);
  return (
    <>
      <SimpleGrid cols={{ base: 1, sm: 2 }}>
        <Autocomplete
          label={t('stock.production.fabricType')}
          withAsterisk
          data={recipe.fabricTypes}
          value={draft.fabricType}
          onChange={setFabricType}
          comboboxProps={{ withinPortal: true, hideDetached: false }}
        />
        <TextInput
          label={t('stock.production.netKg')}
          inputMode="decimal"
          withAsterisk
          {...field('netKg')}
        />
      </SimpleGrid>
      {recipe.hasRecipe && (
        <Button variant="light" w="fit-content" onClick={recipe.fill}>
          {t('stock.production.fillRecipe')}
        </Button>
      )}
      {recipe.hasFailed && <Alert color="red">{t('stock.production.fillFailed')}</Alert>}
    </>
  );
}
