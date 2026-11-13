import { useTranslation } from '@iplik/i18n';
import { Button, Group } from '@iplik/ui';
import { Link } from 'react-router';

export type ChangePasswordActionsProps = {
  /** Geçici şifreyle girenin gidecek başka yeri yoktur; "Vazgeç" gizlenir. */
  isCancelHidden: boolean;
  isPending: boolean;
  canSubmit: boolean;
};

export function ChangePasswordActions({
  isCancelHidden,
  isPending,
  canSubmit,
}: ChangePasswordActionsProps) {
  const { t } = useTranslation();
  return (
    <Group justify="space-between">
      {isCancelHidden ? (
        <span />
      ) : (
        <Button component={Link} to="/" variant="default">
          {t('auth.changePassword.cancel')}
        </Button>
      )}
      <Button type="submit" loading={isPending} disabled={!canSubmit}>
        {t('auth.changePassword.submit')}
      </Button>
    </Group>
  );
}
