import type { LoginAccountView } from '@iplik/api-client';
import { useCurrentUser } from '@/modules/auth';
import { hasChangeableRole } from '../staffRoles';
import { ChangeRoleButton } from './ChangeRoleButton';

/**
 * Hesap satırının işlemleri. Şoför/fasoncu hesabında ve kişinin kendi hesabında rol değiştirme sunulmaz;
 * sunucu da aynı kuralı uygular (kendi yetkisini düşürüp kilitlenmeyi önler).
 */
export function AccountActions({ account }: { account: LoginAccountView }) {
  const { data: me } = useCurrentUser();
  if (!hasChangeableRole(account) || me?.userId === account.id) return null;
  return <ChangeRoleButton account={account} />;
}
