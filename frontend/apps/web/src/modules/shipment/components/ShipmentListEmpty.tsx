import { useTranslation } from '@iplik/i18n';
import { Button, EmptyState } from '@iplik/ui';
import { Link } from 'react-router';
import { usePermission } from '@/modules/auth';

export type ShipmentListEmptyProps = {
  /** Durum süzgeci seçiliyse liste o yüzden boştur. */
  isFiltered: boolean;
  onShowAll: () => void;
};

/**
 * Boş sevkiyat listesi ne yapılacağını söyler: süzgeç seçiliyse "tüm durumları göster", hiç sevkiyat yoksa
 * açabilen kullanıcıya "Yeni sevkiyat" düğmesi, açamayana sevkiyatın kimden geleceği.
 */
export function ShipmentListEmpty({ isFiltered, onShowAll }: ShipmentListEmptyProps) {
  const { t } = useTranslation();
  const canCreate = usePermission('SHIPMENT_CREATE');
  if (isFiltered) {
    return (
      <EmptyState
        title={t('shipment.list.empty')}
        action={
          <Button variant="default" onClick={onShowAll}>
            {t('shipment.list.showAll')}
          </Button>
        }
      />
    );
  }
  return (
    <EmptyState
      title={t('shipment.list.none')}
      description={t(canCreate ? 'shipment.list.noneHintCreate' : 'shipment.list.noneHint')}
      action={
        canCreate ? (
          <Button component={Link} to="/panel/sevkiyat/yeni">
            {t('create.open')}
          </Button>
        ) : undefined
      }
    />
  );
}
