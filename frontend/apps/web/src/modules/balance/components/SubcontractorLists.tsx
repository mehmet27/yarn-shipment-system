import { useTranslation } from '@iplik/i18n';
import { formatKgText } from '@/modules/shipment';
import { SubcontractorListCard as ListCard } from './SubcontractorListCard';
import type { SubcontractorOverview } from '../hooks/useSubcontractorOverview';

/** Fasoncunun açık işleri: yoldaki sevkiyatlar, kumaşı beklenenler, faturalanacaklar ve açık uyuşmazlıklar. */
export function SubcontractorLists({ overview }: { overview: SubcontractorOverview }) {
  const { t } = useTranslation();
  return (
    <>
      <ListCard
        title={t('subcontractorPage.inTransit.title')}
        icon="shipments"
        empty={t('subcontractorPage.inTransit.empty')}
        rows={overview.inTransit.map((shipment) => ({
          to: `/panel/sevkiyat/${shipment.shipmentId}`,
          shipmentNo: shipment.shipmentNo,
          description: [shipment.driverName, shipment.plateNo].filter(Boolean).join(' · '),
          color: 'yellow',
        }))}
      />
      <ListCard
        title={t('subcontractorPage.awaitingFabric.title')}
        icon="lots"
        empty={t('subcontractorPage.awaitingFabric.empty')}
        rows={overview.awaitingFabric.map((row) => ({
          to: `/panel/sevkiyat/${row.shipmentId}?sekme=kumas`,
          shipmentNo: row.shipmentNo,
          description: t('subcontractorPage.awaitingFabric.row', {
            kg: formatKgText(row.expectedFabricKg),
          }),
          color: 'yellow',
        }))}
      />
      <ListCard
        title={t('subcontractorPage.toInvoice.title')}
        icon="balances"
        empty={t('subcontractorPage.toInvoice.empty')}
        rows={overview.toInvoice.map((row) => ({
          to: `/panel/sevkiyat/${row.shipmentId}?sekme=fire`,
          shipmentNo: row.shipmentNo,
          description: t('subcontractorPage.toInvoice.row', { kg: formatKgText(row.remainingKg) }),
          color: 'red',
        }))}
      />
      {overview.canSeeDiscrepancies && (
        <ListCard
          title={t('subcontractorPage.discrepancies.title')}
          icon="discrepancies"
          empty={t('subcontractorPage.discrepancies.empty')}
          rows={overview.openDiscrepancies.map((item) => ({
            to: `/panel/uyusmazliklar/${item.id}`,
            shipmentNo: item.discrepancyNo,
            description: t(`discrepancy.status.${item.status}`),
            color: item.severity === 'RED' ? 'red' : 'yellow',
          }))}
        />
      )}
    </>
  );
}
