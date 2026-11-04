import { Paper, Stack, Text } from '@iplik/ui';

export type StatCardProps = {
  label: string;
  value: string | number;
  hint?: string | undefined;
  color?: string | undefined;
  testId?: string | undefined;
};

/** Tek göstergelik kart; renk anlamı yalnızca vurgudur, etiket her zaman yazılıdır (FE-UI-03). */
const DEFAULT_COLOR = 'dark.9';

export function StatCard({ label, value, hint, color = DEFAULT_COLOR, testId }: StatCardProps) {
  return (
    <Paper withBorder p="md" data-testid={testId}>
      <Stack gap={2}>
        <Text size="sm" c="dimmed">
          {label}
        </Text>
        <Text fw={700} fz="xl" c={color}>
          {value}
        </Text>
        {hint === undefined ? null : (
          <Text size="xs" c="dimmed">
            {hint}
          </Text>
        )}
      </Stack>
    </Paper>
  );
}
