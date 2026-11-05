import type { PackageType } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert } from '@iplik/ui';

/** Üretim sonrası bilgi: kaç adet hangi ambalajdan oluşturuldu. */
export function CreatedNotice({ count, packageType }: { count: number; packageType: PackageType }) {
  const { t } = useTranslation();
  return (
    <Alert color="green" role="status">
      {t('bag.created', { count, item: t(`bag.packageTypesLower.${packageType}`) })}
    </Alert>
  );
}
