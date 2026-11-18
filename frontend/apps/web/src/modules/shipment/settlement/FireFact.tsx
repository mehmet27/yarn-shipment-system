import { Stack, Text } from '@iplik/ui';

const FACT_STYLE = {
  border: '1px solid var(--app-line)',
  borderRadius: 'var(--mantine-radius-md)',
};

export function FireFact({ label, value }: { label: string; value: string }) {
  return (
    <Stack gap={0} p="sm" bg="var(--app-inset)" style={FACT_STYLE}>
      <Text size="sm" c="dimmed">
        {label}
      </Text>
      <Text fw={600} style={{ whiteSpace: 'nowrap' }}>
        {value}
      </Text>
    </Stack>
  );
}
