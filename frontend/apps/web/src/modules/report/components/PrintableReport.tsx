import { useTranslation } from '@iplik/i18n';
import { Table, Text, Title } from '@iplik/ui';
import { PrintSignatures } from './PrintSignatures';
import { formatCell } from '../export/formatCell';
import type { ReportTable } from '../export/reportModel';
import '../print.css';

export type PrintableReportProps = {
  tables: ReportTable[];
  title: string;
  logo?: string | null;
};

/** Yalnızca yazdırırken görünür; tarayıcıda "PDF olarak kaydet" ile PDF çıktısı bu bölümden alınır. */
export function PrintableReport({ tables, title, logo = null }: PrintableReportProps) {
  const { t } = useTranslation();
  return (
    <div className="print-only" data-testid="printable-report">
      {logo !== null && <img src={logo} alt="" className="print-logo" />}
      <Title order={1} size="h2">
        {title}
      </Title>
      {tables.map((table) => (
        <section key={table.title} className="print-section">
          <Title order={2} size="h4">
            {table.title}
          </Title>
          {table.rows.length === 0 ? (
            <Text size="sm">{t('report.parties.empty')}</Text>
          ) : (
            <Table withTableBorder withColumnBorders>
              <Table.Thead>
                <Table.Tr>
                  {table.headers.map((header) => (
                    <Table.Th key={header}>{header}</Table.Th>
                  ))}
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {table.rows.map((row) => (
                  <Table.Tr key={row.map(formatCell).join('|')}>
                    {row.map((cell, index) => (
                      <Table.Td key={table.headers[index]}>{formatCell(cell)}</Table.Td>
                    ))}
                  </Table.Tr>
                ))}
                {table.total !== undefined && (
                  <Table.Tr fw={700}>
                    {table.total.map((cell, index) => (
                      <Table.Td key={table.headers[index]}>{formatCell(cell)}</Table.Td>
                    ))}
                  </Table.Tr>
                )}
              </Table.Tbody>
            </Table>
          )}
        </section>
      ))}
      <Text size="xs" mt="md">
        {t('report.intro')}
      </Text>
      <PrintSignatures />
    </div>
  );
}
