import { useTranslation } from '@iplik/i18n';
import { Stack, Text } from '@iplik/ui';
import type { RecipeLineDraft } from './createDraft';
import { FabricTypeSelect } from './FabricTypeSelect';
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
      <FabricTypeSelect
        label={t('create.recipe.fabricType')}
        value={line.fabricType}
        onChange={fabric.change}
      />
      {fabric.isYarnOutsideRecipe && (
        <Text size="xs" c="orange">
          {t('create.recipe.yarnOutsideRecipe')}
        </Text>
      )}
    </Stack>
  );
}
