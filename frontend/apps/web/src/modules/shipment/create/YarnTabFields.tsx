import { useTranslation } from '@iplik/i18n';
import { SimpleGrid, TextInput } from '@iplik/ui';
import type { RecipeLineDraft } from './createDraft';
import { priceValid } from './createDraft';
import { MasterSelect } from './MasterSelect';
import { YarnCell } from './YarnCell';

type YarnTabFieldsProps = {
  line: RecipeLineDraft;
  index: number;
  error: string | undefined;
  onPatch: (patch: Partial<RecipeLineDraft>) => void;
  supplierId?: string;
};

/** Satırın İplik sekmesi: cins (Tanımlar'dan seçilir), o ipliğin partisi ve net kg. */
export function YarnTabFields({ line, index, error, onPatch, supplierId }: YarnTabFieldsProps) {
  const { t } = useTranslation();
  return (
    <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="sm">
      <YarnCell line={line} index={index} onPatch={onPatch} />
      <MasterSelect
        kind="lot"
        label={t('create.recipe.lot')}
        value={line.lot}
        {...(line.yarn === undefined ? {} : { productId: line.yarn.value })}
        {...(supplierId === undefined ? {} : { supplierId })}
        onChange={(lot) => {
          onPatch({ lot });
        }}
      />
      <TextInput
        label={`${t('create.recipe.netKg')} ${String(index + 1)}`}
        inputMode="decimal"
        withAsterisk
        value={line.netKg}
        {...(error === undefined || error === 'linePriceInvalid'
          ? {}
          : { error: t(`create.errors.${error}`) })}
        onChange={(event) => {
          onPatch({ netKg: event.currentTarget.value });
        }}
      />
      <TextInput
        label={t('create.recipe.unitPrice')}
        withAsterisk
        inputMode="decimal"
        value={line.unitPrice}
        {...(error !== undefined && !priceValid(line.unitPrice)
          ? { error: t('create.errors.linePriceInvalid') }
          : {})}
        onChange={(event) => {
          onPatch({ unitPrice: event.currentTarget.value });
        }}
      />
    </SimpleGrid>
  );
}
