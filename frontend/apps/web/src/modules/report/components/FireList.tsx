import type { FireRowView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Stack, Text } from '@iplik/ui';
import { useState } from 'react';
import { fireTable } from '../export/faconTables';
import { FireTable } from './FireTable';
import { PanelExportBar } from './PanelExportBar';
import { optionsOf } from './subcontractorOptions';

type FireListProps = {
  rows: FireRowView[];
  title: string;
  fileName: string;
  withStatus: boolean;
};

export function FireList({ rows, title, fileName, withStatus }: FireListProps) {
  const { t } = useTranslation();
  const [subcontractorId, setSubcontractorId] = useState<string | null>(null);
  const visible =
    subcontractorId === null ? rows : rows.filter((row) => row.subcontractorId === subcontractorId);
  return (
    <Stack>
      <PanelExportBar
        tables={[fireTable(visible, t, { title, withStatus })]}
        fileName={fileName}
        options={optionsOf(rows)}
        subcontractorId={subcontractorId}
        onSubcontractorChange={setSubcontractorId}
      />
      {visible.length === 0 ? (
        <Text c="dimmed">{t('report.fire.empty')}</Text>
      ) : (
        <FireTable rows={visible} withStatus={withStatus} />
      )}
    </Stack>
  );
}
