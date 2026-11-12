import { Paper, Stack, Text } from '@iplik/ui';
import type { ReactNode } from 'react';

/** Başlıklı çerçeveli form kartı. */
export function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <Paper withBorder p="md" radius="md">
      <Stack gap="sm">
        <Text fw={700}>{title}</Text>
        {children}
      </Stack>
    </Paper>
  );
}
