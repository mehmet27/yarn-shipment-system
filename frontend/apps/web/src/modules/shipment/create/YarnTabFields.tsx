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

/** Satırın İplik sekmesi: cins (Tanımlar'dan seçilir), o ipliğin partisi ve net kg. */
export function YarnTabFields({ line, index, error, onPatch }: YarnTabFieldsProps) {
  const { t } = useTranslation();
  return (
    <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="sm">
      <YarnCell line={line} index={index} onPatch={onPatch} />
      <MasterSelect
        kind="lot"
        label={t('create.recipe.lot')}
        value={line.lot}
        {...(line.yarn === undefined ? {} : { productId: line.yarn.value })}
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
