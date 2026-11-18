import { Stack, Text } from '@mantine/core';
import type { ReactNode } from 'react';

export type EmptyStateProps = {
  title: string;
  description?: string;
  action?: ReactNode;
};

/** Boş liste ve "kaydedilmedi" durumları için tek tip gösterim. */
export function EmptyState({ title, description, action }: EmptyStateProps) {
  return (
    <Stack
      align="center"
      gap="xs"
      py="xl"
      px="md"
      ta="center"
      style={{
        border: '1px dashed var(--app-control-line)',
        borderRadius: 'var(--mantine-radius-md)',
      }}
    >
      <Text fw={600}>{title}</Text>
      {description === undefined ? null : (
        <Text size="sm" c="dimmed">
          {description}
        </Text>
      )}
      {action}
    </Stack>
  );
}
