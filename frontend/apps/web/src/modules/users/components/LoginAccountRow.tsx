import type { LoginAccountView } from '@iplik/api-client';
import { formatDateTime, useTranslation } from '@iplik/i18n';
import { Table, Text } from '@iplik/ui';
import { AccountActions } from './AccountActions';
import { RoleBadges } from './RoleBadges';
import { StatusBadges } from './StatusBadges';

/** Tablo satırı: kullanıcı adı, ad, e-posta, rol, durum, açılış ve işlemler. Şifre sütunu yoktur. */
export function LoginAccountRow({ account }: { account: LoginAccountView }) {
  const { t } = useTranslation();
  return (
    <Table.Tr>
      <Table.Td>
        <Text fw={600} size="sm">
          {account.username}
        </Text>
      </Table.Td>
      <Table.Td>
        {account.fullName ?? (
          <Text c="dimmed" size="sm">
            {t('users.noName')}
          </Text>
        )}
      </Table.Td>
      <Table.Td>{account.email ?? '–'}</Table.Td>
      <Table.Td>
        <RoleBadges account={account} />
      </Table.Td>
      <Table.Td>
        <StatusBadges account={account} />
      </Table.Td>
      <Table.Td>
        {account.createdAt === undefined ? '–' : formatDateTime(account.createdAt)}
      </Table.Td>
      <Table.Td>
        <AccountActions account={account} />
      </Table.Td>
    </Table.Tr>
  );
}
