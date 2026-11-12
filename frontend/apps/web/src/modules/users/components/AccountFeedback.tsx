import { useTranslation } from '@iplik/i18n';
import { Alert, Stack } from '@iplik/ui';

export type AccountFeedbackProps = { isSaved: boolean; errorCode: string | undefined };

/** Satır işleminin sonucu: kısa süreli "Kaydedildi" ya da hata iletisi. */
export function AccountFeedback({ isSaved, errorCode }: AccountFeedbackProps) {
  const { t } = useTranslation();
  return (
    <Stack gap={4}>
      {isSaved && (
        <Alert color="green" role="status">
          {t('users.saved')}
        </Alert>
      )}
      {errorCode !== undefined && (
        <Alert color="red">
          {t(errorCode === 'LOGIN_ACCOUNT_NOT_FOUND' ? 'users.notFound' : 'users.disable.error')}
        </Alert>
      )}
    </Stack>
  );
}
