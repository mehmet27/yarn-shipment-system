import { useTranslation } from '@iplik/i18n';
import { Alert } from '@iplik/ui';
import { SaveConfirm } from '@/modules/shared';
import { stockErrorKey } from '../stockErrors';
import type { useProductionSubmit } from '../useProductionSubmit';

type Props = Pick<
  ReturnType<typeof useProductionSubmit>,
  'error' | 'mutation' | 'step' | 'submit' | 'confirm'
>;

/** Merkez kumaş girişinin uyarıları ve iki adımlı kaydet düğmesi. */
export function ProductionActions({ error, mutation, step, submit, confirm }: Props) {
  const { t } = useTranslation();
  return (
    <>
      {error !== undefined && <Alert color="red">{t(`stock.draftErrors.${error}`)}</Alert>}
      {mutation.isSuccess && error === undefined && (
        <Alert color="green">{t('stock.production.saved')}</Alert>
      )}
      {mutation.isError && <Alert color="red">{t(stockErrorKey(mutation.error))}</Alert>}
      <SaveConfirm
        isOpen={step.isOpen}
        isLoading={mutation.isPending}
        submitLabel={t('stock.production.submit')}
        onSubmit={submit}
        onConfirm={confirm}
        onCancel={step.close}
      />
    </>
  );
}
