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
      <Button onClick={onAdd} w={{ base: '100%', sm: 'fit-content' }}>
        {t('masterData.add')}
      </Button>
      <TextInput
        maw={{ base: '100%', sm: 420 }}
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
