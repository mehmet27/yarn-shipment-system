import type { FabricRecipe } from '@iplik/api-client/fabric';
import { useTranslation } from '@iplik/i18n';
import { Button, EmptyState, Stack, Text } from '@iplik/ui';
import { QueryState } from '@/modules/shared';
import { useState } from 'react';
import { useAllFabricRecipes, useSaveFabricRecipe } from '../hooks/useRecipes';
import { RecipeCard } from './RecipeCard';
import { RecipeForm } from './RecipeForm';

/** "Kumaş karışımı" sekmesi: kumaş cinsi başına varsayılan iplik karışımı; kumaş girişinde oranlar buradan dolar. */
export function RecipesPanel() {
  const { t } = useTranslation();
  const query = useAllFabricRecipes();
  const [editing, setEditing] = useState<FabricRecipe | 'new'>();
  const toggle = useSaveFabricRecipe();
  const close = () => {
    setEditing(undefined);
  };
  return (
    <Stack>
      <Text size="sm" c="dimmed">
        {t('masterData.recipes.intro')}
      </Text>
      {editing === undefined ? (
        <Button
          w="fit-content"
          onClick={() => {
            setEditing('new');
          }}
        >
          {t('masterData.recipes.add')}
        </Button>
      ) : (
        <RecipeForm
          key={editing === 'new' ? 'new' : editing.id}
          recipe={editing === 'new' ? undefined : editing}
          onClose={close}
        />
      )}
      <QueryState query={query}>
        {(recipes) =>
          recipes.length === 0 ? (
            <EmptyState title={t('masterData.recipes.empty')} />
          ) : (
            <Stack>
              {recipes.map((recipe) => (
                <RecipeCard
                  key={recipe.id}
                  recipe={recipe}
                  isBusy={toggle.isPending}
                  onEdit={() => {
                    setEditing(recipe);
                  }}
                  onToggle={() => {
                    const { id, fabricType, yarns } = recipe;
                    toggle.mutate({
                      id,
                      request: { fabricType, yarns, active: !recipe.active },
                    });
                  }}
                />
              ))}
            </Stack>
          )
        }
      </QueryState>
    </Stack>
  );
}
