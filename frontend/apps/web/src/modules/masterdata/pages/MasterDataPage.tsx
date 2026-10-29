import { useTranslation } from '@iplik/i18n';
import { Stack, Tabs, Title } from '@iplik/ui';
import { RecordList } from '../components/RecordList';
import { MANAGED_KINDS } from '../kinds';

/** Yönetici "Tanımlar": fasoncu, şoför, araç ve lokasyon ekleme, düzenleme, pasife alma. */
export function MasterDataPage() {
  const { t } = useTranslation();
  return (
    <Stack>
      <Title order={1} size="h2">
        {t('masterData.title')}
      </Title>
      <Tabs defaultValue={MANAGED_KINDS[0]} keepMounted={false}>
        <Tabs.List>
          {MANAGED_KINDS.map((kind) => (
            <Tabs.Tab key={kind} value={kind}>
              {t(`masterData.kinds.${kind}.tab`)}
            </Tabs.Tab>
          ))}
        </Tabs.List>
        {MANAGED_KINDS.map((kind) => (
          <Tabs.Panel key={kind} value={kind} pt="md">
            <RecordList kind={kind} />
          </Tabs.Panel>
        ))}
      </Tabs>
    </Stack>
  );
}
