import { useTranslation } from '@iplik/i18n';
import { Stack, TextInput } from '@iplik/ui';
import type { RecipeLineDraft } from './createDraft';
import { MasterSelect } from './MasterSelect';

type YarnCellProps = {
  line: RecipeLineDraft;
  index: number;
  onPatch: (patch: Partial<RecipeLineDraft>) => void;
};

/** İplik cinsi: listeden seçim ya da serbest metin; seçim varken metin kapalıdır. */
export function YarnCell({ line, index, onPatch }: YarnCellProps) {
  const { t } = useTranslation();
  return (
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
    </Stack>
  );
}
