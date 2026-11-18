import type { LoginAccountView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Group, Text } from '@iplik/ui';

export type PasswordRequestsNoticeProps = {
  accounts: readonly LoginAccountView[];
  onShow: () => void;
};

/** Şifre isteyen hesap varsa yöneticiye üstte haber verir; "Göster" süzgeci o hesaplara çevirir. */
export function PasswordRequestsNotice({ accounts, onShow }: PasswordRequestsNoticeProps) {
  const { t } = useTranslation();
  const requesting = accounts.filter((account) => account.passwordRequestedAt !== undefined);
  if (requesting.length === 0) return null;
  return (
    <Alert color="red" title={t('users.requests.title', { count: requesting.length })}>
      <Group justify="space-between" wrap="nowrap">
        <Text size="sm">
          {t('users.requests.names', {
            names: requesting.map((account) => account.fullName ?? account.username).join(', '),
          })}
        </Text>
        <Button size="xs" variant="default" onClick={onShow}>
          {t('users.requests.show')}
        </Button>
      </Group>
    </Alert>
  );
}
