import { Box, Group, List, Text } from '@mantine/core';

export type ActivityItem = {
  id: string;
  title: string;
  /** Tarih ve kullanıcı gibi ikincil bilgi. */
  meta: string;
};

export type ActivityFeedProps = {
  items: readonly ActivityItem[];
  label: string;
};

/** Etkinlik akışı: en yeni üstte olacak şekilde sıralanmış olayları listeler. */
export function ActivityFeed({ items, label }: ActivityFeedProps) {
  return (
    <List listStyleType="none" aria-label={label} spacing="sm" p={0}>
      {items.map((item) => (
        <List.Item key={item.id}>
          <Group gap="sm" wrap="nowrap" align="flex-start">
            <Box aria-hidden w={10} h={10} mt={6} bg="brand.7" style={{ borderRadius: '50%' }} />
            <Box>
              <Text fw={600}>{item.title}</Text>
              <Text size="xs" c="dimmed">
                {item.meta}
              </Text>
            </Box>
          </Group>
        </List.Item>
      ))}
    </List>
  );
}
