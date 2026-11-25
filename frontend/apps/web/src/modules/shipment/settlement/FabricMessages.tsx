import { useTranslation } from '@iplik/i18n';
import { Alert } from '@iplik/ui';
import { settlementErrorKey } from './settlementErrors';
import type { useFabricSubmit } from './useFabricSubmit';

/** Taslak hatası, kontrol ve kayıt hataları ile kayıt başarısı. */
export function FabricMessages({ form }: { form: ReturnType<typeof useFabricSubmit> }) {
  const { t } = useTranslation();
  const { error, mutation, checked, checkError } = form;
  return (
    <>
      {error !== undefined && <Alert color="red">{t(`settlement.draftErrors.${error}`)}</Alert>}
      {mutation.isSuccess && checked === undefined && (
        <Alert color="green">{t('settlement.fabric.saved')}</Alert>
      )}
      {checkError !== null && <Alert color="red">{t(settlementErrorKey(checkError))}</Alert>}
      {mutation.isError && <Alert color="red">{t(settlementErrorKey(mutation.error))}</Alert>}
    </>
  );
}
