import { Group, Text } from '@mantine/core';

export type ListGroupHeaderProps = {
  label: string;
  count: number;
};

/** Gruplu liste başlığı: "İşlem bekleyen  4". */
export function ListGroupHeader({ label, count }: ListGroupHeaderProps) {
  return (
    <Group justify="space-between" px="md" py="xs" bg="var(--app-inset)">
      <Text size="sm" fw={600}>
        {label}
      </Text>
      <Text size="sm" fw={600} c="dimmed">
        {count}
      </Text>
    </Group>
  );
}
