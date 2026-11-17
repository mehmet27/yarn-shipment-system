import type { listDiscrepancies } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Menu, Text } from '@iplik/ui';
import { Link } from 'react-router';

type DiscrepancyItems = NonNullable<Awaited<ReturnType<typeof listDiscrepancies>>>['items'];

export function DiscrepancyMenuItems({ items }: { items: DiscrepancyItems }) {
  const { t } = useTranslation();
  return items.map((item) => (
    <Menu.Item key={item.id} component={Link} to={`/panel/uyusmazliklar/${item.id}`}>
      <Text size="sm" fw={600}>
        {item.discrepancyNo}
      </Text>
      <Text size="xs" c="dimmed">
        {t(`discrepancy.type.${item.type}`)}
      </Text>
    </Menu.Item>
  ));
}
