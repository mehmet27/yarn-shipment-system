import { useTranslation } from '@iplik/i18n';
import { Alert } from '@iplik/ui';
import { settlementErrorKey } from './settlementErrors';
import type { useFabricSubmit } from './useFabricSubmit';

/** Taslak hatası, sunucu hatası ve kayıt başarısı; kilo taşması alanın altında zaten yazılıdır. */
export function FabricMessages({ form }: { form: ReturnType<typeof useFabricSubmit> }) {
  const { t } = useTranslation();
  const { error, mutation, isOverLimit, isConfirming } = form;
  return (
    <>
      {error !== undefined && !isOverLimit && (
        <Alert color="red">{t(`settlement.draftErrors.${error}`)}</Alert>
      )}
      {mutation.isSuccess && !isConfirming && (
        <Alert color="green">{t('settlement.fabric.saved')}</Alert>
      )}
      {mutation.isError && <Alert color="red">{t(settlementErrorKey(mutation.error))}</Alert>}
    </>
  );
}
