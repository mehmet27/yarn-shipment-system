import type { LoginAccountView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Divider, Modal, Stack } from '@iplik/ui';
import { ChangeNameForm } from './ChangeNameForm';
import { ChangeRoleForm } from './ChangeRoleForm';

export type EditAccountModalProps = {
  account: LoginAccountView;
  opened: boolean;
  onClose: () => void;
  onSaved?: () => void;
  /** Rol yalnız kişinin kendisi dışındaki fabrika içi hesapta değişir; kendi hesabında yalnız ad soyad sunulur. */
  canChangeRole: boolean;
};

/** Tek pencerede ad soyad ve rol düzenleme; her bölümün kendi kaydet düğmesi vardır. */
export function EditAccountModal({
  account,
  opened,
  onClose,
  onSaved,
  canChangeRole,
}: EditAccountModalProps) {
  const { t } = useTranslation();
  const done = () => {
    onClose();
    onSaved?.();
  };
  return (
    <Modal opened={opened} onClose={onClose} title={t('users.edit.title')}>
      <Stack>
        <ChangeNameForm account={account} onClose={done} />
        {canChangeRole && (
          <>
            <Divider />
            <ChangeRoleForm account={account} onClose={done} />
          </>
        )}
      </Stack>
    </Modal>
  );
}
