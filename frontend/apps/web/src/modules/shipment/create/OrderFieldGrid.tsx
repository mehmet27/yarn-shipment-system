import { useTranslation } from '@iplik/i18n';
import { Select, SimpleGrid, TextInput } from '@iplik/ui';
import type { RecipeLineDraft } from './createDraft';
import { priceValid } from './createDraft';

type OrderFieldGridProps = {
  line: RecipeLineDraft;
  showErrors: boolean;
  onPatch: (patch: Partial<RecipeLineDraft>) => void;
};

type TextField = 'fabricType' | 'lycra' | 'yarnLength' | 'pussFine';

/** Sipariş formu alanları: kumaş cinsi, LYC, en türü, iplik uzunluğu, puss-fine, birim fiyat. */
export function OrderFieldGrid({ line, showErrors, onPatch }: OrderFieldGridProps) {
  const { t } = useTranslation();
  const text = (field: TextField) => (
    <TextInput
      label={t(`create.recipe.${field}`)}
      value={line[field]}
      maxLength={200}
      onChange={(event) => {
        onPatch({ [field]: event.currentTarget.value });
      }}
    />
  );
  return (
    <SimpleGrid cols={{ base: 1, sm: 3 }} spacing="xs">
      {text('fabricType')}
      {text('lycra')}
      <Select
        label={t('create.recipe.widthType')}
        clearable
        data={[
          { value: 'TUBE', label: t('create.recipe.widthTypes.TUBE') },
          { value: 'MAY', label: t('create.recipe.widthTypes.MAY') },
          { value: 'OPEN_WIDTH', label: t('create.recipe.widthTypes.OPEN_WIDTH') },
        ]}
        value={line.widthType === '' ? null : line.widthType}
        onChange={(value) => {
          onPatch({ widthType: value ?? '' });
        }}
      />
      {text('yarnLength')}
      {text('pussFine')}
      <TextInput
        label={t('create.recipe.unitPrice')}
        inputMode="decimal"
        value={line.unitPrice}
        {...(showErrors && !priceValid(line.unitPrice)
          ? { error: t('create.errors.linePriceInvalid') }
          : {})}
        onChange={(event) => {
          onPatch({ unitPrice: event.currentTarget.value });
        }}
      />
    </SimpleGrid>
  );
}
