import { useTranslation } from '@iplik/i18n';
import { Button, Group, Icon, Text } from '@iplik/ui';
import { usePanelExport } from '../hooks/usePanelExport';
import type { ReportTable } from '../export/reportModel';

/** Excel ve PDF düğmeleri tek satırda, sekme çubuğunun yanına konmak üzere; hata aynı satırda kısa metin olur. */
export function InlineExportButtons({
  tables,
  fileName,
}: {
  tables: ReportTable[];
  fileName: string;
}) {
  const { t } = useTranslation();
  const { hasFailed, run } = usePanelExport(tables, fileName);
  return (
    <Group gap="xs" wrap="wrap" ml="auto" pb={4}>
      {hasFailed && (
        <Text size="xs" c="red" role="alert">
          {t('report.export.failed')}
        </Text>
      )}
      <Button
        size="compact-sm"
        variant="default"
        leftSection={<Icon name="excel" size={14} />}
        onClick={() => void run('xlsx')}
      >
        {t('report.export.excel')}
      </Button>
      <Button
        size="compact-sm"
        variant="default"
        leftSection={<Icon name="pdf" size={14} />}
        onClick={() => void run('pdf')}
      >
        {t('report.export.pdf')}
      </Button>
    </Group>
  );
}
