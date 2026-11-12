import type { LoginAccountView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Button, Group, Modal, Stack, Text } from '@iplik/ui';

export type DisableAccountModalProps = {
  account: LoginAccountView;
  opened: boolean;
  loading: boolean;
  onClose: () => void;
  onConfirm: () => void;
};

/** Pasife alma geri alınabilir ama kişiyi hemen dışarı attığı için önce onay istenir. */
export function DisableAccountModal({
  account,
  opened,
  loading,
  onClose,
  onConfirm,
}: DisableAccountModalProps) {
  const { t } = useTranslation();
  return (
    <Modal opened={opened} onClose={onClose} title={t('users.disable.confirmTitle')}>
      <Stack>
        <Text size="sm">{t('users.disable.confirmIntro', { username: account.username })}</Text>
        <Group justify="flex-end">
          <Button variant="default" onClick={onClose}>
            {t('users.disable.cancel')}
          </Button>
          <Button color="red" loading={loading} onClick={onConfirm}>
            {t('users.disable.confirm')}
          </Button>
        </Group>
      </Stack>
    </Modal>
  );
}
