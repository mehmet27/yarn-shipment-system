import type { ListShipmentsParams, ShipmentSummary } from '@iplik/api-client';
import { listShipments } from '@iplik/api-client';
import { formatDate, formatDateTime } from '@iplik/i18n';
import type { ReportTable } from '@/modules/report';
import { text } from '@/modules/report';

const EXPORT_PAGE_SIZE = 100;
/** En çok 5.000 sevkiyat aktarılır (rapor dökümüyle aynı sınır); fazlası için süzgeç daraltılır. */
const MAX_EXPORT_PAGES = 50;

type Translate = (key: string) => string;

/** Süzgece uyan tüm sevkiyatlar (Excel için); sayfa sayfa okunur. */
export async function fetchAllShipments(
  params: Omit<ListShipmentsParams, 'page' | 'size'>,
): Promise<ShipmentSummary[]> {
  const rows: ShipmentSummary[] = [];
  for (let page = 0; page < MAX_EXPORT_PAGES; page += 1) {
    const result = await listShipments({ ...params, page, size: EXPORT_PAGE_SIZE });
    rows.push(...result.items);
    if (page + 1 >= result.page.totalPages) break;
  }
  return rows;
}

/** Sevkiyat listesinin Excel sayfası; sütunlar ekrandaki tabloyla aynıdır. */
export function shipmentListTable(
  shipments: readonly ShipmentSummary[],
  t: Translate,
): ReportTable {
  return {
    title: t('shipment.list.title'),
    headers: [
      t('shipment.field.shipmentNo'),
      t('shipment.field.status'),
      t('shipment.nextStep.label'),
      t('shipment.field.subcontractor'),
      t('shipment.field.driver'),
      t('shipment.field.plate'),
      t('shipment.field.shipmentDate'),
      t('shipment.field.createdAt'),
    ],
    rows: shipments.map((shipment) => [
      text(shipment.shipmentNo),
      text(t(`shipment.status.${shipment.status}`)),
      text(t(`shipment.nextStep.${shipment.status}`)),
      text(shipment.subcontractorTitle),
      text(shipment.driverName ?? ''),
      text(shipment.plateNo ?? ''),
      text(formatDate(shipment.shipmentDate)),
      text(formatDateTime(shipment.createdAt)),
    ]),
  };
}
