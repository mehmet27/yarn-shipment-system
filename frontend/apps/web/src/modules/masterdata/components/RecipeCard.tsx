import type { FabricRecipe } from '@iplik/api-client/fabric';
import { useTranslation } from '@iplik/i18n';
import { Badge, Button, Group, Paper, Stack, Text } from '@iplik/ui';

export type RecipeCardProps = {
  recipe: FabricRecipe;
  isBusy: boolean;
  onEdit: () => void;
  onToggle: () => void;
};

/** Bir kumaş cinsinin karışımı: iplikler ve oranlar, düzenle ve pasife al düğmeleri. */
export function RecipeCard({ recipe, isBusy, onEdit, onToggle }: RecipeCardProps) {
  const { t } = useTranslation();
  const mix = recipe.yarns.map((y) => `${y.yarnType} %${y.ratioPct.replace('.', ',')}`).join(' · ');
  return (
    <Paper withBorder p="md" radius="md" component="article">
      <Group justify="space-between" align="flex-start" wrap="nowrap">
        <Stack gap={2}>
          <Group gap="xs">
            <Text fw={600}>{recipe.fabricType}</Text>
            {!recipe.active && <Badge color="gray">{t('masterData.inactive')}</Badge>}
          </Group>
          <Text size="sm" c="dimmed">
            {mix}
          </Text>
        </Stack>
        <Group gap="xs" wrap="nowrap">
          <Button variant="default" size="xs" onClick={onEdit}>
            {t('masterData.edit')}
          </Button>
          <Button variant="default" size="xs" loading={isBusy} onClick={onToggle}>
            {t(recipe.active ? 'masterData.deactivate' : 'masterData.activate')}
          </Button>
        </Group>
      </Group>
    </Paper>
  );
}
