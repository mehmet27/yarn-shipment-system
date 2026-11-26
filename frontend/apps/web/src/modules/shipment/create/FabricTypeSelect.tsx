import { useTranslation } from '@iplik/i18n';
import { Select } from '@iplik/ui';
import { useActiveFabricRecipes } from '../settlement/hooks';

type FabricTypeSelectProps = {
  label: string;
  value: string;
  onChange: (fabricType: string) => void;
  isRequired?: boolean;
};

/**
 * Kumaş cinsi: yalnızca Tanımlar > Kumaş karışımı'ndaki cinslerden seçilir (iplikteki kuralın aynısı). Eskiden
 * yazılmış bir ad varsa kaybolmasın diye listede tutulur.
 */
export function FabricTypeSelect({
  label,
  value,
  onChange,
  isRequired = false,
}: FabricTypeSelectProps) {
  const { t } = useTranslation();
  const names = (useActiveFabricRecipes().data ?? []).map((recipe) => recipe.fabricType);
  const data = value === '' || names.includes(value) ? names : [value, ...names];
  return (
    <Select
      label={label}
      description={t('create.fabricTypeHint')}
      placeholder={t('create.selectPlaceholder')}
      data={data}
      value={value === '' ? null : value}
      onChange={(next) => {
        onChange(next ?? '');
      }}
      searchable
      clearable={!isRequired}
      allowDeselect={!isRequired}
      withAsterisk={isRequired}
      nothingFoundMessage="—"
      comboboxProps={{ withinPortal: true, hideDetached: false }}
    />
  );
}
