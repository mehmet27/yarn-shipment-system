import { useTranslation } from '@iplik/i18n';
import { Button, Stack } from '@iplik/ui';

/** Şifreyi belirle düğmesi ve kodu yeniden isteme bağlantısı. */
export function ConfirmActions({
  isPending,
  onResend,
}: {
  isPending: boolean;
  onResend: () => void;
}) {
  const { t } = useTranslation();
  return (
    <Stack>
      <Button type="submit" loading={isPending}>
        {t('forgotPassword.confirmSubmit')}
      </Button>
      <Button variant="subtle" onClick={onResend}>
        {t('forgotPassword.resend')}
      </Button>
    </Stack>
  );
}
