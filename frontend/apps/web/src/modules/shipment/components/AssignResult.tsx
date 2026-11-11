import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert } from '@iplik/ui';

/** Çuval ekleme sonucu: hata varsa durum ya da genel mesaj, yoksa eklenen sayı; ikisi de yoksa boş. */
export function AssignResult({ error, count }: { error: Error | null; count: number | undefined }) {
  const { t } = useTranslation();
  if (error !== null) {
    const code = toProblem(error).code;
    return (
      <Alert color="red">
        {t(`shipment.bags.assign.errors.${code === 'INVALID_SHIPMENT_STATUS' ? code : 'unknown'}`)}
      </Alert>
    );
  }
  if (count === undefined) return null;
  return <Alert color="green">{t('shipment.bags.assign.done', { count })}</Alert>;
}
