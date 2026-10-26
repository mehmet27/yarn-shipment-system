import { useTranslation } from '@iplik/i18n';
import { Modal, useMediaQuery } from '@iplik/ui';
import type { LoginAccountKind } from '../hooks/useCreateLoginAccount';
import { LoginAccountForm } from './LoginAccountForm';

/** Dar ekranda tam ekran açılır; kapatılınca geçici şifre bir daha görülemez. */
export function LoginAccountDialog({
  kind,
  recordId,
  onClose,
}: {
  kind: LoginAccountKind;
  recordId: string;
  onClose: () => void;
}) {
  const { t } = useTranslation();
  const isNarrow = useMediaQuery('(max-width: 47.99em)', false);
  return (
    <Modal
      opened
      onClose={onClose}
      fullScreen={isNarrow}
      title={t('masterData.loginAccount.title')}
    >
      <LoginAccountForm kind={kind} recordId={recordId} onClose={onClose} />
    </Modal>
  );
}
