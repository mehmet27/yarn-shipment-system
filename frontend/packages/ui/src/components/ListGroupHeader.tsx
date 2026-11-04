import { Group, Text } from '@mantine/core';

export type ListGroupHeaderProps = {
  label: string;
  count: number;
};

/** Gruplu liste başlığı: "İŞLEM BEKLEYEN  4". */
export function ListGroupHeader({ label, count }: ListGroupHeaderProps) {
  return (
    <Group justify="space-between" px="md" py="xs" bg="gray.0">
      <Text size="xs" fw={700} c="dimmed" tt="uppercase" lts="0.06em">
        {label}
      </Text>
      <Text size="xs" fw={700} c="dimmed">
        {count}
      </Text>
    </Group>
  );
}
