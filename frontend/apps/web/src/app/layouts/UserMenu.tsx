import { changePasswordUrl, logout } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Avatar, Group, Menu, NavIcon, Text, UnstyledButton } from '@iplik/ui';
import { useCurrentUser } from '@/modules/auth';
import { UserMenuProfile } from './UserMenuProfile';

const AVATAR_SIZE_PX = 44;
const MENU_WIDTH_PX = 280;

async function logoutAndRedirect(): Promise<void> {
  window.location.assign(await logout());
}

function initialsOf(fullName: string): string {
  return fullName
    .split(/\s+/)
    .filter((part) => part !== '')
    .slice(0, 2)
    .map((part) => part.charAt(0).toLocaleUpperCase('tr-TR'))
    .join('');
}

/** Üst çubuğun sağı: kişinin adı ve rolü; tıklayınca bilgi kartı, şifre değiştirme ve çıkış. */
export function UserMenu() {
  const { t } = useTranslation();
  const { data: user } = useCurrentUser();
  if (user === undefined) {
    return null;
  }
  return (
    <Menu position="bottom-end" width={MENU_WIDTH_PX} shadow="lg" radius="md">
      <Menu.Target>
        <UnstyledButton aria-label={user.fullName} c="white">
          <Group gap="xs" wrap="nowrap">
            <Avatar size={AVATAR_SIZE_PX} radius="xl" color="white" c="brand.7">
              {initialsOf(user.fullName)}
            </Avatar>
            <Text size="sm" fw={600} visibleFrom="sm" lh={1.2}>
              {user.fullName}
            </Text>
          </Group>
        </UnstyledButton>
      </Menu.Target>
      <Menu.Dropdown>
        <UserMenuProfile user={user} />
        <Menu.Divider />
        <Menu.Item
          leftSection={<NavIcon name="password" />}
          onClick={() => {
            window.location.assign(changePasswordUrl());
          }}
        >
          {t('app.changePassword')}
        </Menu.Item>
        <Menu.Item
          color="red"
          leftSection={<NavIcon name="logout" />}
          onClick={() => void logoutAndRedirect()}
        >
          {t('app.logout')}
        </Menu.Item>
      </Menu.Dropdown>
    </Menu>
  );
}
