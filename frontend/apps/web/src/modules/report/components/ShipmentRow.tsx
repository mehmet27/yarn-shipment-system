import type { ShipmentRowView } from '@iplik/api-client';
import { formatDateTime, useTranslation } from '@iplik/i18n';
import { Anchor, Table } from '@iplik/ui';
import { Link } from 'react-router';
import { formatKilograms } from '../format';
import { usePartyNames } from '../hooks/usePartyNames';

const DASH = '—';

function orDash(value: string | undefined, format: (value: string) => string): string {
  return value === undefined ? DASH : format(value);
}

/** Sevkiyat dökümünün tek satırı; eksik değerler çizgiyle gösterilir. */
export function ShipmentRow({ row }: { row: ShipmentRowView }) {
  const { t } = useTranslation();
  const { nameOf } = usePartyNames();
  return (
    <Table.Tr>
      <Table.Td>
        <Anchor component={Link} to={`/panel/sevkiyat/${row.shipmentId}`}>
          {row.shipmentNo}
        </Anchor>
      </Table.Td>
      <Table.Td>{nameOf(row.subcontractorId)}</Table.Td>
      <Table.Td>{t(`shipment.status.${row.status}`)}</Table.Td>
      <Table.Td>{orDash(row.dispatchNetKg, formatKilograms)}</Table.Td>
      <Table.Td>{orDash(row.deliveryNetKg, formatKilograms)}</Table.Td>
      <Table.Td>{formatDateTime(row.createdAt)}</Table.Td>
    </Table.Tr>
  );
}
