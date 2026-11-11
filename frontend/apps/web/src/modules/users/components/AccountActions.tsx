import type { LoginAccountView } from '@iplik/api-client';
import { Group } from '@iplik/ui';
import { useCurrentUser } from '@/modules/auth';
import { hasChangeableRole } from '../staffRoles';
import { ChangeRoleButton } from './ChangeRoleButton';
import { SetPasswordButton } from './SetPasswordButton';
import { ToggleEnabledButton } from './ToggleEnabledButton';

/**
 * Hesap satırının işlemleri: yeni geçici şifre verme, pasife alma/açma ve fabrika içi hesapta rol değiştirme. Kişinin
 * kendi hesabında pasife alma ve rol değiştirme sunulmaz; sunucu da aynı kuralı uygular (kilitlenmeyi önler).
 */
export function AccountActions({ account }: { account: LoginAccountView }) {
  const { data: me } = useCurrentUser();
  const isOwn = me?.userId === account.id;
  return (
    <Group gap="xs" align="flex-start" wrap="nowrap">
      <SetPasswordButton account={account} />
      {!isOwn && <ToggleEnabledButton account={account} />}
      {!isOwn && hasChangeableRole(account) && <ChangeRoleButton account={account} />}
    </Group>
  );
}
