import { useTranslation } from '@iplik/i18n';
import { Autocomplete, Stack, Text } from '@iplik/ui';
import type { RecipeLineDraft } from './createDraft';
import { useOrderFabric } from './useOrderFabric';

type FabricTypeFieldProps = {
  line: RecipeLineDraft;
  onPatch: (patch: Partial<RecipeLineDraft>) => void;
};

export function FabricTypeField({ line, onPatch }: FabricTypeFieldProps) {
  const { t } = useTranslation();
  const fabric = useOrderFabric(line, onPatch);
  return (
    <Stack gap={4}>
      <Autocomplete
        label={t('create.recipe.fabricType')}
        data={fabric.fabricTypes}
        value={line.fabricType}
        maxLength={200}
        onChange={fabric.change}
        comboboxProps={{ withinPortal: true, hideDetached: false }}
      />
      {fabric.isYarnOutsideRecipe && (
        <Text size="xs" c="orange">
          {t('create.recipe.yarnOutsideRecipe')}
        </Text>
      )}
    </Stack>
  );
}
