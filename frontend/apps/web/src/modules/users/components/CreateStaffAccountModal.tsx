import { useTranslation } from '@iplik/i18n';
import { Modal } from '@iplik/ui';
import { CreateStaffAccountForm } from './CreateStaffAccountForm';

export type CreateStaffAccountModalProps = { opened: boolean; onClose: () => void };

/** Fabrika içi hesap açma penceresi; kapanınca içerik sökülür, form her açılışta temiz başlar. */
export function CreateStaffAccountModal({ opened, onClose }: CreateStaffAccountModalProps) {
  const { t } = useTranslation();
  return (
    <Modal opened={opened} onClose={onClose} title={t('users.create.title')}>
      <CreateStaffAccountForm onClose={onClose} />
    </Modal>
  );
}
