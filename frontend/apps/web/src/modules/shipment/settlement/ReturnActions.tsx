import { useTranslation } from '@iplik/i18n';
import { Alert } from '@iplik/ui';
import { SaveConfirm } from '@/modules/shared';
import { settlementErrorKey } from './settlementErrors';
import type { useReturnSubmit } from './useReturnSubmit';

/** İplik dönüşünün uyarıları ve iki adımlı kaydet düğmesi. */
export function ReturnActions({
  error,
  mutation,
  step,
  submit,
  confirm,
}: ReturnType<typeof useReturnSubmit>) {
  const { t } = useTranslation();
  return (
    <>
      {error !== undefined && <Alert color="red">{t(`settlement.draftErrors.${error}`)}</Alert>}
      {mutation.isError && <Alert color="red">{t(settlementErrorKey(mutation.error))}</Alert>}
      <SaveConfirm
        isOpen={step.isOpen}
        isLoading={mutation.isPending}
        submitLabel={t('settlement.yarn.submit')}
        onSubmit={submit}
        onConfirm={confirm}
        onCancel={step.close}
      />
    </>
  );
}
