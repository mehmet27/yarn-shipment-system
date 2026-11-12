import type { LoginAccountView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { ActionIcon, Menu } from '@iplik/ui';

const MENU_ICON = '⋯';

export type AccountMenuProps = {
  account: LoginAccountView;
  isOwn: boolean;
  onPassword: () => void;
  onDisable: () => void;
  onEnable: () => void;
};

/** "⋯" menüsü: yeni geçici şifre, pasife alma ve yeniden açma; kendi hesabında pasife alma/açma yoktur. */
export function AccountMenu({ account, isOwn, onPassword, onDisable, onEnable }: AccountMenuProps) {
  const { t } = useTranslation();
  const { username } = account;
  return (
    <Menu position="bottom-end" withinPortal>
      <Menu.Target>
        <ActionIcon variant="default" size="lg" aria-label={t('users.menu.open', { username })}>
          {MENU_ICON}
        </ActionIcon>
      </Menu.Target>
      <Menu.Dropdown>
        <Menu.Item onClick={onPassword} aria-label={t('users.setPassword.open', { username })}>
          {t('users.setPassword.action')}
        </Menu.Item>
        {!isOwn && account.enabled && (
          <Menu.Item
            color="red"
            onClick={onDisable}
            aria-label={t('users.disable.open', { username })}
          >
            {t('users.disable.action')}
          </Menu.Item>
        )}
        {!isOwn && !account.enabled && (
          <Menu.Item onClick={onEnable} aria-label={t('users.enable.open', { username })}>
            {t('users.enable.action')}
          </Menu.Item>
        )}
      </Menu.Dropdown>
    </Menu>
  );
}
