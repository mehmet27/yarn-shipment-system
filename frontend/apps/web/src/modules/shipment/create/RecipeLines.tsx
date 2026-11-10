import { useTranslation } from '@iplik/i18n';
import { Button, Group, Stack, Text } from '@iplik/ui';
import { formatKgText } from '@iplik/units';
import type { RecipeLineDraft } from './createDraft';
import { newLine, totalNetKg } from './createDraft';
import { RecipeLineRow } from './RecipeLineRow';

export type RecipeLinesProps = {
  lines: readonly RecipeLineDraft[];
  onChange: (lines: readonly RecipeLineDraft[]) => void;
  /** Gönderim denendikten sonra satır hataları görünür. */
  showErrors: boolean;
};

/** Reçete satırları: iplik cinsi (liste ya da serbest metin), isteğe bağlı lot, net kg; en az bir satır. */
export function RecipeLines({ lines, onChange, showErrors }: RecipeLinesProps) {
  const { t } = useTranslation();
  const update = (key: string, patch: Partial<RecipeLineDraft>) => {
    onChange(lines.map((line) => (line.key === key ? { ...line, ...patch } : line)));
  };
  return (
    <Stack gap="sm" data-testid="recipe-lines">
      <Text fw={600}>{t('create.recipe.title')}</Text>
      {lines.map((line, index) => (
        <RecipeLineRow
          key={line.key}
          line={line}
          index={index}
          removable={lines.length > 1}
          showErrors={showErrors}
          onPatch={(patch) => {
            update(line.key, patch);
          }}
          onRemove={() => {
            onChange(lines.filter((candidate) => candidate.key !== line.key));
          }}
        />
      ))}
      <Group justify="space-between">
        <Button
          variant="default"
          onClick={() => {
            onChange([...lines, newLine()]);
          }}
        >
          {t('create.recipe.add')}
        </Button>
        <Text fw={700} data-testid="recipe-total">
          {t('create.recipe.total', { total: formatKgText(totalNetKg(lines)) })}
        </Text>
      </Group>
    </Stack>
  );
}
