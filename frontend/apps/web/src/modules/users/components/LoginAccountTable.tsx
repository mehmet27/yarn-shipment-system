import type { LoginAccountView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Stack, Table, useMediaQuery } from '@iplik/ui';
import { LoginAccountCard } from './LoginAccountCard';
import { LoginAccountRow } from './LoginAccountRow';

const DESKTOP_QUERY = '(min-width: 48em)';
const TABLE_MIN_WIDTH = 900;

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
            <Table.Th>{t('users.field.phone')}</Table.Th>
            <Table.Th>{t('users.field.roles')}</Table.Th>
            <Table.Th>{t('users.field.status')}</Table.Th>
            <Table.Th>{t('users.field.createdAt')}</Table.Th>
            <Table.Th>{t('users.field.actions')}</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {accounts.map((account) => (
            <LoginAccountRow key={account.id} account={account} />
          ))}
        </Table.Tbody>
      </Table>
    </Table.ScrollContainer>
  );
}
