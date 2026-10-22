import { useTranslation } from '@iplik/i18n';
import { Button, Stack, Switch, TextInput } from '@iplik/ui';

export type RecordToolbarProps = {
  search: string;
  isShowingInactive: boolean;
  onAdd: () => void;
  onSearch: (search: string) => void;
  onToggleInactive: (isShowing: boolean) => void;
};

/** Ekle düğmesi, arama ve "pasifleri göster"; hepsi tek sütunda, parmakla erişilir. */
export function RecordToolbar({
  search,
  isShowingInactive,
  onAdd,
  onSearch,
  onToggleInactive,
}: RecordToolbarProps) {
  const { t } = useTranslation();
  return (
    <Stack>
      <Button onClick={onAdd}>{t('masterData.add')}</Button>
      <TextInput
        placeholder={t('masterData.search')}
        value={search}
        onChange={(event) => {
          onSearch(event.currentTarget.value);
        }}
      />
      <Switch
        label={t('masterData.showInactive')}
        checked={isShowingInactive}
        onChange={(event) => {
          onToggleInactive(event.currentTarget.checked);
        }}
      />
    </Stack>
  );
}
