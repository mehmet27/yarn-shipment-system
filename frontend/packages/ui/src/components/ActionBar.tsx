import { Group, Paper } from '@mantine/core';
import type { ReactNode } from 'react';

export type ActionBarProps = {
  children: ReactNode;
};

/** Ana eylemleri ekranın altında sabit tutar; telefonda tek elle erişim için. */
export function ActionBar({ children }: ActionBarProps) {
  return (
    <Paper
      pos="sticky"
      bottom={0}
      p="sm"
      withBorder
      shadow="md"
      style={{ zIndex: 5 }}
      data-testid="action-bar"
    >
      <Group justify="flex-end" gap="sm" wrap="wrap">
        {children}
      </Group>
    </Paper>
  );
}
