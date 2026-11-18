import { Anchor, Box, Group, Text } from '@iplik/ui';
import { Link } from 'react-router';

const DOT_SIZE = 8;

export type PendingRowProps = {
  to: string;
  shipmentNo: string;
  description: string;
  /** Noktanın rengi: kırmızı karar bekleyen, sarı bekleyen iş. */
  color: 'red' | 'yellow';
};

/** "Sizi bekleyenler" satırı: solda renkli nokta, kayıt no, soluk açıklama. */
export function PendingRow({ to, shipmentNo, description, color }: PendingRowProps) {
  return (
    <Group gap="sm" wrap="nowrap" py={4}>
      <Box
        aria-hidden
        w={DOT_SIZE}
        h={DOT_SIZE}
        style={{
          flexShrink: 0,
          borderRadius: '50%',
          background: `var(--mantine-color-${color}-filled)`,
        }}
      />
      <Anchor component={Link} to={to} fw={600} c="var(--app-text)" underline="hover">
        {shipmentNo}
      </Anchor>
      <Text size="sm" c="dimmed" truncate>
        {description}
      </Text>
    </Group>
  );
}
