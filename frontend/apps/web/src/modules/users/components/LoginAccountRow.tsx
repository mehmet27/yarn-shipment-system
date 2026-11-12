import type { LoginAccountView } from '@iplik/api-client';
import { Table } from '@iplik/ui';
import { AccountActions } from './AccountActions';
import { AccountIdentity } from './AccountIdentity';
import { RoleBadges } from './RoleBadges';
import { StatusBadges } from './StatusBadges';

/** Tablo satırı: kimlik (ad soyad + giriş adı), rol, durum ve işlemler; pasif hesap soluk görünür. Şifre sütunu yoktur. */
export function LoginAccountRow({ account }: { account: LoginAccountView }) {
  return (
    <Table.Tr {...(account.enabled ? {} : { style: { opacity: 0.65 } })}>
      <Table.Td>
        <AccountIdentity account={account} />
      </Table.Td>
      <Table.Td>
        <RoleBadges account={account} />
      </Table.Td>
      <Table.Td>
        <StatusBadges account={account} />
      </Table.Td>
      <Table.Td>
        <AccountActions account={account} />
      </Table.Td>
    </Table.Tr>
  );
}
