import type { LoginAccountView } from '@iplik/api-client';
import { formatDateTime, useTranslation } from '@iplik/i18n';
import { Stack, Table, Text, useMediaQuery } from '@iplik/ui';
import { RoleBadges } from './RoleBadges';
import { StatusBadges } from './StatusBadges';
import { LoginAccountCard } from './LoginAccountCard';

const DESKTOP_QUERY = '(min-width: 48em)';
const TABLE_MIN_WIDTH = 720;

export type LoginAccountTableProps = { accounts: readonly LoginAccountView[] };

/** Mobil öncelikli (FE-UI-07): dar ekranda kartlar, geniş ekranda tablo. Şifre sütunu yoktur. */
export function LoginAccountTable({ accounts }: LoginAccountTableProps) {
  const { t } = useTranslation();
  const isDesktop = useMediaQuery(DESKTOP_QUERY, true, { getInitialValueInEffect: false });
  if (!isDesktop) {
    return (
      <Stack gap="sm">
        {accounts.map((account) => (
          <LoginAccountCard key={account.id} account={account} />
        ))}
      </Stack>
    );
  }
  return (
    <Table.ScrollContainer minWidth={TABLE_MIN_WIDTH}>
      <Table striped highlightOnHover>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>{t('users.field.username')}</Table.Th>
            <Table.Th>{t('users.field.fullName')}</Table.Th>
            <Table.Th>{t('users.field.email')}</Table.Th>
            <Table.Th>{t('users.field.roles')}</Table.Th>
            <Table.Th>{t('users.field.status')}</Table.Th>
            <Table.Th>{t('users.field.createdAt')}</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {accounts.map((account) => (
            <Table.Tr key={account.id}>
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
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
    </Table.ScrollContainer>
  );
}
