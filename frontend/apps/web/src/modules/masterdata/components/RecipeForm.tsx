import type { FabricRecipe } from '@iplik/api-client/fabric';
import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Group, SectionCard, Stack, Text, TextInput } from '@iplik/ui';
import { useState } from 'react';
import { useSaveFabricRecipe } from '../hooks/useRecipes';
import { useRecords } from '../hooks/useRecords';
import type { RecipeDraft } from '../recipeDraft';
import { EMPTY_RECIPE_DRAFT, recipeTotal, toRecipeRequest } from '../recipeDraft';
import { RecipeLines } from './RecipeLines';

const PRODUCT_PAGE_SIZE = 100;
const FULL = 100;

function draftOf(recipe: FabricRecipe | undefined): RecipeDraft {
  if (recipe === undefined) return EMPTY_RECIPE_DRAFT;
  return {
    fabricType: recipe.fabricType,
    lines: recipe.yarns.map((y) => ({ yarnType: y.yarnType, ratio: y.ratioPct })),
  };
}

export type RecipeFormProps = {
  recipe: FabricRecipe | undefined;
  onClose: () => void;
};

/** Kumaş cinsinin varsayılan iplik karışımı: iplik listesinden seçilir, oranların toplamı tam %100 olur. */
export function RecipeForm({ recipe, onClose }: RecipeFormProps) {
  const { t } = useTranslation();
  const [draft, setDraft] = useState(() => draftOf(recipe));
  const products = useRecords('product', {
    q: '',
    page: 0,
    size: PRODUCT_PAGE_SIZE,
    isShowingInactive: false,
  });
  const names = [...new Set((products.data?.rows ?? []).map((row) => row.label))];
  const save = useSaveFabricRecipe();
  const request = toRecipeRequest(draft);
  const total = recipeTotal(draft);
  const submit = () => {
    if (request === undefined) return;
    const active = recipe === undefined ? {} : { active: recipe.active };
    save.mutate({ id: recipe?.id, request: { ...request, ...active } }, { onSuccess: onClose });
  };
  const isDuplicate = save.isError && toProblem(save.error).code === 'FABRIC_RECIPE_EXISTS';
  return (
    <SectionCard title={t('masterData.recipes.add')}>
      <Stack>
        <TextInput
          label={t('masterData.recipes.fabricType')}
          withAsterisk
          value={draft.fabricType}
          onChange={(event) => {
            const { value } = event.currentTarget;
            setDraft((current) => ({ ...current, fabricType: value }));
          }}
        />
        <RecipeLines
          lines={draft.lines}
          names={names}
          onChange={(lines) => {
            setDraft((current) => ({ ...current, lines }));
          }}
        />
        <Text size="sm" fw={600} c={total === FULL ? 'green' : 'red'} data-testid="recipe-total">
          {t(total === FULL ? 'masterData.recipes.totalOk' : 'masterData.recipes.total', {
            total: String(total).replace('.', ','),
          })}
        </Text>
        {save.isError && (
          <Alert color="red">
            {t(isDuplicate ? 'masterData.recipes.exists' : 'masterData.saveFailed')}
          </Alert>
        )}
        <Group>
          <Button disabled={request === undefined} loading={save.isPending} onClick={submit}>
            {t('masterData.save')}
          </Button>
          <Button variant="default" onClick={onClose}>
            {t('masterData.recipes.cancel')}
          </Button>
        </Group>
      </Stack>
    </SectionCard>
  );
}
