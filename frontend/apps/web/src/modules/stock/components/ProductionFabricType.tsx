import { useTranslation } from '@iplik/i18n';
import { Alert, SimpleGrid, TextInput } from '@iplik/ui';
import { FabricTypeSelect } from '@/modules/shipment';
import { useProductionRecipe } from '../hooks/useProductionRecipe';
import type { YarnLineDraft } from '../stockDraft';
import type { useProductionSubmit } from '../useProductionSubmit';

type ProductionState = ReturnType<typeof useProductionSubmit>;

type Props = Pick<ProductionState, 'draft' | 'field' | 'setFabricType'> & {
  setYarns: (rows: YarnLineDraft[]) => void;
};

/** Kumaş cinsi (tanımlı karışımlardan) ve toplam kumaş kilosu; cins iplikleri belirler. */
export function ProductionFabricType({ draft, field, setFabricType, setYarns }: Props) {
  const { t } = useTranslation();
  const recipe = useProductionRecipe(draft.fabricType, { setFabricType, setYarns });
  return (
    <>
      <SimpleGrid cols={{ base: 1, sm: 2 }}>
        <FabricTypeSelect
          label={t('stock.production.fabricType')}
          value={draft.fabricType}
          onChange={recipe.change}
          isRequired
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
