import { useTranslation } from '@iplik/i18n';
import { Table, Text, Title } from '@iplik/ui';
import { formatCell } from '../export/formatCell';
import type { ReportTable } from '../export/reportModel';
import '../print.css';

export type PrintableReportProps = {
  tables: ReportTable[];
  title: string;
};

const SIGNATURES = ['prepared', 'checked', 'approved'] as const;

/** Yalnızca yazdırırken görünür; tarayıcıda "PDF olarak kaydet" ile PDF çıktısı bu bölümden alınır. */
export function PrintableReport({ tables, title }: PrintableReportProps) {
  const { t } = useTranslation();
  return (
    <div className="print-only" data-testid="printable-report">
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
      <div className="print-signatures">
        {SIGNATURES.map((key) => (
          <div key={key} className="print-signature">
            <Text size="sm" fw={600}>
              {t(`report.export.signature.${key}`)}
            </Text>
            <div className="print-signature-line" />
            <Text size="xs">{t('report.export.signature.dateLine')}</Text>
          </div>
        ))}
      </div>
    </div>
  );
}
