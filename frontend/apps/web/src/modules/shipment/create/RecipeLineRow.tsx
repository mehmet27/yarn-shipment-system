import { useTranslation } from '@iplik/i18n';
import { Button, Paper, Stack, TextInput } from '@iplik/ui';
import type { RecipeLineDraft } from './createDraft';
import { lineError } from './createDraft';
import { MasterSelect } from './MasterSelect';

type RecipeLineRowProps = {
  line: RecipeLineDraft;
  index: number;
  removable: boolean;
  showErrors: boolean;
  onPatch: (patch: Partial<RecipeLineDraft>) => void;
  onRemove: () => void;
};

export function RecipeLineRow({
  line,
  index,
  removable,
  showErrors,
  onPatch,
  onRemove,
}: RecipeLineRowProps) {
  const { t } = useTranslation();
  const error = showErrors ? lineError(line) : undefined;
  const label = t('create.recipe.remove', { index: index + 1 });
  return (
    <Paper withBorder p="sm" radius="md">
      <Stack gap="xs">
        <MasterSelect
          kind="product"
          label={`${t('create.recipe.yarnType')} ${String(index + 1)}`}
          value={line.yarn}
          onChange={(yarn) => {
            onPatch({ yarn });
          }}
        />
        <TextInput
          label={t('create.recipe.yarnTypeText')}
          value={line.yarnText}
          disabled={line.yarn !== undefined}
          maxLength={200}
          onChange={(event) => {
            onPatch({ yarnText: event.currentTarget.value });
          }}
        />
        <MasterSelect
          kind="lot"
          label={t('create.recipe.lot')}
          value={line.lot}
          onChange={(lot) => {
            onPatch({ lot });
          }}
        />
        <TextInput
          label={`${t('create.recipe.netKg')} ${String(index + 1)}`}
          inputMode="decimal"
          value={line.netKg}
          {...(error === undefined ? {} : { error: t(`create.errors.${error}`) })}
          onChange={(event) => {
            onPatch({ netKg: event.currentTarget.value });
          }}
        />
        {removable && (
          <Button variant="subtle" color="red" w="fit-content" onClick={onRemove}>
            {label}
          </Button>
        )}
      </Stack>
    </Paper>
  );
}
