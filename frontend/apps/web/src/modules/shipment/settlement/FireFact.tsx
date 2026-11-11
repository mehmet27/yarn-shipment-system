import { Stack, Text } from '@iplik/ui';

export function FireFact({ label, value }: { label: string; value: string }) {
  return (
    <Stack gap={0}>
      <Text size="sm" c="dimmed">
        {label}
      </Text>
      <Text fw={600}>{value}</Text>
    </Stack>
  );
}
