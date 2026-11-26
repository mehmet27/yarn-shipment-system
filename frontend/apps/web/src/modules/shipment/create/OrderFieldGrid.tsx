import { useTranslation } from '@iplik/i18n';
import { Select, SimpleGrid, TextInput } from '@iplik/ui';
import type { RecipeLineDraft } from './createDraft';
import { FabricTypeField } from './FabricTypeField';
import { LycraSelect } from './LycraSelect';

type OrderFieldGridProps = {
  line: RecipeLineDraft;
  onPatch: (patch: Partial<RecipeLineDraft>) => void;
};

type TextField = 'yarnLength' | 'pussFine';

/** Sipariş formu alanları: kumaş cinsi, LYC, en türü, iplik uzunluğu, puss-fine. */
export function OrderFieldGrid({ line, onPatch }: OrderFieldGridProps) {
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
    <SimpleGrid cols={{ base: 1, sm: 3 }} spacing="sm">
      <FabricTypeField line={line} onPatch={onPatch} />
      <LycraSelect
        value={line.lycra}
        onChange={(lycra) => {
          onPatch({ lycra });
        }}
      />
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
    </SimpleGrid>
  );
}
