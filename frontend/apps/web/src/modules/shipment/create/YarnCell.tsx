import { useTranslation } from '@iplik/i18n';
import type { RecipeLineDraft } from './createDraft';
import { MasterSelect } from './MasterSelect';

type YarnCellProps = {
  line: RecipeLineDraft;
  index: number;
  onPatch: (patch: Partial<RecipeLineDraft>) => void;
};

/** İplik cinsi: yalnızca Tanımlar'daki ipliklerden seçilir; serbest metin yoktur (merkez stok cins bazında izlenir). */
export function YarnCell({ line, index, onPatch }: YarnCellProps) {
  const { t } = useTranslation();
  return (
    <MasterSelect
      kind="product"
      isRequired
      label={`${t('create.recipe.yarnType')} ${String(index + 1)}`}
      value={line.yarn}
      onChange={(yarn) => {
        // Parti ipliğe aittir; iplik değişince önceki partinin seçimi kalmaz.
        onPatch(yarn?.value === line.yarn?.value ? { yarn } : { yarn, lot: undefined });
      }}
    />
  );
}
