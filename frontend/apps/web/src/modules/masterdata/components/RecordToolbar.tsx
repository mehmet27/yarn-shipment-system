import { useTranslation } from '@iplik/i18n';
import { Button, Group, Icon, Switch, TextInput } from '@iplik/ui';

export type RecordToolbarProps = {
  search: string;
  isShowingInactive: boolean;
  onAdd: () => void;
  onSearch: (search: string) => void;
  onToggleInactive: (isShowing: boolean) => void;
};

/** Ekle düğmesi (solda, ikonlu), arama ve "pasifleri göster"; dar ekranda alt alta dizilir. */
export function RecordToolbar({
  search,
  isShowingInactive,
  onAdd,
  onSearch,
  onToggleInactive,
}: RecordToolbarProps) {
  const { t } = useTranslation();
  return (
    <Group align="center" gap="md">
      <Button
        onClick={onAdd}
        leftSection={<Icon name="plus" />}
        w={{ base: '100%', sm: 'fit-content' }}
      >
        {t('masterData.add')}
      </Button>
      <TextInput
        flex={{ base: '1 1 100%', sm: '0 1 360px' }}
        leftSection={<Icon name="search" size={16} />}
        placeholder={t('masterData.search')}
        aria-label={t('masterData.search')}
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
    </Group>
  );
}
