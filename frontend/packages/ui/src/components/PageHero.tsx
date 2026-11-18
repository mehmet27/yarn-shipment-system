import { Box, Group, Stack, Text, Title } from '@mantine/core';
import type { ReactNode } from 'react';

export type PageHeroProps = {
  title: string;
  subtitle?: string;
  /** Sağ üst eylemler (örn. "Yeni sevkiyat"). */
  actions?: ReactNode;
  /** Başlığın altındaki içerik (özet kartları). */
  children?: ReactNode;
};

/**
 * Sayfa başlığı: sade başlık, altında tek satır yönlendirme, sağda sayfanın ana eylemi. Zemin kutusu yoktur; başlık
 * doğrudan sayfa zemininde durur, böylece ekrandaki tek dolgulu renk ana eylem düğmesi olur.
 */
export function PageHero({ title, subtitle, actions, children }: PageHeroProps) {
  return (
    <Stack gap="md">
      <Group justify="space-between" align="flex-end" gap="sm">
        <Stack gap={2} miw={0} style={{ flex: '1 1 16rem' }}>
          <Title order={1}>{title}</Title>
          {subtitle === undefined ? null : (
            <Text c="dimmed" size="sm" maw="46rem">
              {subtitle}
            </Text>
          )}
        </Stack>
        {actions}
      </Group>
      {children === undefined ? null : <Box>{children}</Box>}
    </Stack>
  );
}
