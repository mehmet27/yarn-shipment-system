import type { SubcontractorOption } from './PanelExportBar';

export function optionsOf(rows: { subcontractorId: string; subcontractorName: string }[]) {
  const map = new Map<string, string>();
  for (const row of rows) map.set(row.subcontractorId, row.subcontractorName);
  return [...map].map(([value, label]): SubcontractorOption => ({ value, label }));
}
