import { useTranslation } from '@iplik/i18n';
import { AppShell, Group, Title } from '@iplik/ui';
import { Outlet } from 'react-router';
import { LogoutButton } from './LogoutButton';

const HEADER_HEIGHT_PX = 56;

/** Mobil saha kabuğu: tek sütun, büyük butonlar; bağlantı göstergesi offline kuyrukla gelecek. */
export function FieldLayout() {
  const { t } = useTranslation();
  return (
    <AppShell header={{ height: HEADER_HEIGHT_PX }} padding="sm">
      <AppShell.Header>
        <Group h="100%" px="sm" justify="space-between">
          <Title order={5}>{t('nav.field')}</Title>
          <LogoutButton />
        </Group>
      </AppShell.Header>
      <AppShell.Main>
        <Outlet />
      </AppShell.Main>
    </AppShell>
  );
}
