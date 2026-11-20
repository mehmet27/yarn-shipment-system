import { Stack, Text } from '@iplik/ui';

export type StatCardProps = {
  label: string;
  value: string | number;
  hint?: string | undefined;
  color?: string | undefined;
  testId?: string | undefined;
};

const VALUE_SIZE = 24;
const MOBILE_VALUE_SIZE = 18;
const LINE = 'var(--app-line)';

/**
 * Tek göstergelik hücre. Çizgiyi hücrenin çevresindeki 1 px gölge çizer; `StatGrid` hücreler arasında 1 px boşluk
 * bıraktığı için komşu hücre çizgiyi örtmez. Etiket her zaman yazılıdır (FE-UI-03).
 */
export function StatCard({ label, value, hint, color, testId }: StatCardProps) {
  return (
    <Stack
      gap={2}
      p="md"
      bg="var(--app-surface)"
      data-testid={testId}
      style={{ boxShadow: `0 0 0 1px ${LINE}` }}
    >
      <Text size="sm" c="dimmed">
        {label}
      </Text>
      <Text
        fw={600}
        fz={{ base: MOBILE_VALUE_SIZE, sm: VALUE_SIZE }}
        lh={1.3}
        {...(color === undefined ? {} : { c: `var(--mantine-color-${color}-light-color)` })}
      >
        {value}
      </Text>
      {hint === undefined ? null : (
        <Text size="xs" c="dimmed">
          {hint}
        </Text>
      )}
    </Stack>
  );
}
