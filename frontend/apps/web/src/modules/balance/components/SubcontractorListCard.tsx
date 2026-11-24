import { SectionCard, Stack, Text } from '@iplik/ui';
import type { NavIconName } from '@iplik/ui';
import { PendingRow } from '@/modules/shipment';
import type { PendingRowProps } from '@/modules/shipment';

type Row = Pick<PendingRowProps, 'to' | 'shipmentNo' | 'description' | 'color'>;

type ListCardProps = {
  title: string;
  icon: NavIconName;
  empty: string;
  rows: Row[];
};

/** Fasoncu sayfasındaki tek liste kartı: satırlar kayda bağlantıdır; boşsa kısa bir not yazar. */
export function SubcontractorListCard({ title, icon, empty, rows }: ListCardProps) {
  return (
    <SectionCard title={title} icon={icon}>
      {rows.length === 0 ? (
        <Text size="sm" c="dimmed">
          {empty}
        </Text>
      ) : (
        <Stack gap={0}>
          {rows.map((row) => (
            <PendingRow key={row.to} {...row} />
          ))}
        </Stack>
      )}
    </SectionCard>
  );
}
