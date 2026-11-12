import { useTranslation } from '@iplik/i18n';
import { SimpleGrid, TextInput } from '@iplik/ui';
import type { RecipeLineDraft } from './createDraft';
import { MasterSelect } from './MasterSelect';
import { YarnCell } from './YarnCell';

type YarnTabFieldsProps = {
  line: RecipeLineDraft;
  index: number;
  error: string | undefined;
  onPatch: (patch: Partial<RecipeLineDraft>) => void;
};

/** Satırın İplik sekmesi: cins, lot ve net kg yan yana. */
export function YarnTabFields({ line, index, error, onPatch }: YarnTabFieldsProps) {
  const { t } = useTranslation();
  return (
    <SimpleGrid cols={{ base: 1, sm: 3 }} spacing="xs">
      <YarnCell line={line} index={index} onPatch={onPatch} />
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
    </SimpleGrid>
  );
}
