import type { LoginAccountView } from '@iplik/api-client';
import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Button, Group, Stack, useDisclosure } from '@iplik/ui';
import { useCurrentUser } from '@/modules/auth';
import { useFlash } from '../hooks/useFlash';
import { useSetAccountEnabled } from '../hooks/useAccountMutations';
import { hasChangeableRole } from '../staffRoles';
import { AccountFeedback } from './AccountFeedback';
import { AccountMenu } from './AccountMenu';
import { DisableAccountModal } from './DisableAccountModal';
import { EditAccountModal } from './EditAccountModal';
import { SetPasswordModal } from './SetPasswordModal';

/**
 * Hesap satırının işlemleri: fabrika içi hesapta "Düzenle" (ad soyad, rol) ve "⋯" menüsünde yeni geçici şifre ile
 * pasife alma/açma. Kişinin kendi hesabında pasife alma ve rol değiştirme sunulmaz (ad soyadı değiştirilebilir);
 * sunucu da aynı kuralı uygular (kilitlenmeyi önler). Pasife alma onay ister; yeniden açma onaysız çalışır.
 */
export function AccountActions({ account }: { account: LoginAccountView }) {
  const { t } = useTranslation();
  const { data: me } = useCurrentUser();
  const isOwn = me?.userId === account.id;
  const enabledMutation = useSetAccountEnabled(account.id);
  const [isEditing, edit] = useDisclosure(false);
  const [isSettingPassword, password] = useDisclosure(false);
  const [isConfirming, confirm] = useDisclosure(false);
  const flash = useFlash();
  const errorCode = enabledMutation.isError ? toProblem(enabledMutation.error).code : undefined;
  return (
    <Stack gap={4}>
      <Group gap="xs" wrap="nowrap">
        {hasChangeableRole(account) && (
          <Button
            variant="default"
            onClick={edit.open}
            aria-label={t('users.edit.open', { username: account.username })}
          >
            {t('users.edit.action')}
          </Button>
        )}
        <AccountMenu
          account={account}
          isOwn={isOwn}
          onPassword={password.open}
          onDisable={confirm.open}
          onEnable={() => {
            enabledMutation.mutate(true, { onSuccess: flash.show });
          }}
        />
      </Group>
      <EditAccountModal
        account={account}
        opened={isEditing}
        onClose={edit.close}
        onSaved={flash.show}
        canChangeRole={!isOwn}
      />
      <SetPasswordModal account={account} opened={isSettingPassword} onClose={password.close} />
      <DisableAccountModal
        account={account}
        opened={isConfirming}
        loading={enabledMutation.isPending}
        onClose={confirm.close}
        onConfirm={() => {
          enabledMutation.mutate(false, {
            onSuccess: flash.show,
            onSettled: confirm.close,
          });
        }}
      />
      <AccountFeedback isSaved={flash.isVisible} errorCode={errorCode} />
    </Stack>
  );
}
