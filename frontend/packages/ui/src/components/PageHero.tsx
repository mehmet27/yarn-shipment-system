import { Box, Group, Stack, Text, Title } from '@mantine/core';
import type { ReactNode } from 'react';

const OVERLAP_PADDING_PX = 64;
const OVERLAP_OFFSET_PX = 40;

export type PageHeroProps = {
  title: string;
  subtitle?: string;
  /** Sağ üst eylemler (örn. "Yeni sevkiyat"). */
  actions?: ReactNode;
  /** Başlığın altına taşan içerik (özet kartları). */
  children?: ReactNode;
};

/** Sayfa başlığı: marka renginden türeyen gradient zemin; içerik altına taşar. */
export function PageHero({ title, subtitle, actions, children }: PageHeroProps) {
  return (
    <Box>
      <Box
        c="white"
        px="lg"
        pt="lg"
        pb={children === undefined ? 'lg' : OVERLAP_PADDING_PX}
        style={{
          background:
            'linear-gradient(120deg, var(--mantine-color-brand-9), var(--mantine-color-brand-7))',
          borderRadius: 'var(--mantine-radius-lg)',
        }}
      >
        <Group justify="space-between" align="flex-start">
          <Stack gap={2}>
            <Title order={1} fz={{ base: 22, sm: 26 }}>
              {title}
            </Title>
            {subtitle === undefined ? null : (
              <Text c="brand.1" size="sm">
                {subtitle}
              </Text>
            )}
          </Stack>
          {actions}
        </Group>
      </Box>
      {children === undefined ? null : <Box mt={-OVERLAP_OFFSET_PX}>{children}</Box>}
    </Box>
  );
}
