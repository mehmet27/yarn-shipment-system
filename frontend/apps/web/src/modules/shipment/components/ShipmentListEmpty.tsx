import { useTranslation } from '@iplik/i18n';
import { Button, EmptyState } from '@iplik/ui';
import { Link } from 'react-router';
import { usePermission } from '@/modules/auth';

export type ShipmentListEmptyProps = {
  /** Süzgeç seçiliyse liste o yüzden boştur. */
  isFiltered: boolean;
  /** Yalnızca durum süzgeci seçili (arama metni ya da tarih yok). */
  isOnlyStatus: boolean;
  onShowAll: () => void;
};

/**
 * Boş sevkiyat listesi ne yapılacağını söyler: süzgeç seçiliyse süzgeci kaldıran düğme, hiç sevkiyat yoksa
 * açabilen kullanıcıya "Yeni sevkiyat" düğmesi, açamayana sevkiyatın kimden geleceği.
 */
export function ShipmentListEmpty({ isFiltered, isOnlyStatus, onShowAll }: ShipmentListEmptyProps) {
  const { t } = useTranslation();
  const canCreate = usePermission('SHIPMENT_CREATE');
  if (isFiltered) {
    return (
      <EmptyState
        title={t(isOnlyStatus ? 'shipment.list.empty' : 'shipment.list.emptyFiltered')}
        action={
          <Button variant="default" onClick={onShowAll}>
            {t(isOnlyStatus ? 'shipment.list.showAll' : 'shipment.list.clearFilters')}
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
