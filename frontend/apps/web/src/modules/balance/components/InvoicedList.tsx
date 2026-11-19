import type { FireInvoicePage } from '@iplik/api-client/settlements';
import { formatDate, useTranslation } from '@iplik/i18n';
import { Anchor, Paper, Stack, Table, Text } from '@iplik/ui';
import { formatKgText, ListPagination } from '@/modules/shipment';
import { Link } from 'react-router';

/** Faturalananlar tablosu: fatura no, tarih, fasoncu, sevkiyat (dosyaya bağlantı) ve kilo. */
export function InvoicedList({
  data,
  page,
  onPage,
}: {
  data: FireInvoicePage;
  page: number;
  onPage: (page: number) => void;
}) {
  const { t } = useTranslation();
  const { items, page: info, totalKg } = data;
  if (items.length === 0) return <Text c="dimmed">{t('settlement.invoiced.empty')}</Text>;
  return (
    <Stack>
      <Text fw={600}>
        {t('settlement.invoiced.totalLine', {
          count: info.totalElements,
          kg: formatKgText(totalKg),
        })}
      </Text>
      <Paper withBorder style={{ overflow: 'hidden' }}>
        <Table.ScrollContainer minWidth={560}>
          <Table aria-label={t('settlement.invoiced.title')}>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>{t('settlement.invoiced.invoiceNo')}</Table.Th>
                <Table.Th>{t('settlement.invoiced.date')}</Table.Th>
                <Table.Th>{t('settlement.invoiced.subcontractor')}</Table.Th>
                <Table.Th>{t('settlement.invoiced.shipment')}</Table.Th>
                <Table.Th ta="right">{t('settlement.invoiced.kg')}</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {items.map((row) => (
                <Table.Tr key={`${row.shipmentId}-${row.invoiceNo}`}>
                  <Table.Td>{row.invoiceNo}</Table.Td>
                  <Table.Td>{formatDate(row.invoicedOn)}</Table.Td>
                  <Table.Td>{row.subcontractorTitle}</Table.Td>
                  <Table.Td>
                    <Anchor component={Link} to={`/panel/sevkiyat/${row.shipmentId}`}>
                      {row.shipmentNo}
                    </Anchor>
                  </Table.Td>
                  <Table.Td ta="right">{formatKgText(row.kg)}</Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
      </Paper>
      {info.totalPages > 1 && (
        <ListPagination total={info.totalPages} value={page} onChange={onPage} />
      )}
    </Stack>
  );
}
