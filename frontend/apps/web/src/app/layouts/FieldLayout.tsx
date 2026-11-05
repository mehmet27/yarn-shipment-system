import { useTranslation } from '@iplik/i18n';
import { AppShell, BRAND_GRADIENT, Group, Text } from '@iplik/ui';
import { Outlet } from 'react-router';
import { UserMenu } from './UserMenu';

const HEADER_HEIGHT_PX = 56;

/** Mobil saha kabuğu: tek sütun, büyük butonlar; bağlantı göstergesi offline kuyrukla gelecek. */
export function FieldLayout() {
  const { t } = useTranslation();
  return (
    <AppShell header={{ height: HEADER_HEIGHT_PX }} padding="sm">
      <AppShell.Header c="white" style={{ background: BRAND_GRADIENT }}>
        <Group h="100%" px="sm" justify="space-between">
          <Text fw={700}>{t('nav.field')}</Text>
          <UserMenu />
        </Group>
      </AppShell.Header>
      <AppShell.Main>
        <Outlet />
      </AppShell.Main>
    </AppShell>
  );
}
