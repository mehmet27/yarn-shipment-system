import type { LoginAccountView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Group } from '@iplik/ui';
import { DotBadge } from '@iplik/ui';

export type StatusBadgesProps = { account: LoginAccountView };

/** Etkin / devre dışı ve ilk girişini bekleyen hesap. */
export function StatusBadges({ account }: StatusBadgesProps) {
  const { t } = useTranslation();
  return (
    <Group gap={4}>
      <DotBadge color={account.enabled ? 'green' : 'gray'}>
        {t(account.enabled ? 'users.status.enabled' : 'users.status.disabled')}
      </DotBadge>
      {account.passwordRequestedAt !== undefined && (
        <DotBadge color="red">{t('users.status.passwordRequested')}</DotBadge>
      )}
      {account.passwordChangeRequired && (
        <DotBadge color="yellow">{t('users.status.passwordChangeRequired')}</DotBadge>
      )}
    </Group>
  );
}
