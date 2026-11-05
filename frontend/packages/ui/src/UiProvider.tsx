import { MantineProvider } from '@mantine/core';
import type { ReactNode } from 'react';
import { useMemo } from 'react';
import { createAppTheme } from './theme';

export type UiProviderProps = {
  children: ReactNode;
  /** Testlerde geçiş animasyonlarını ve portal'ları kapatır. */
  env?: 'default' | 'test';
  /** Kurulumun marka rengi (`#rrggbb`); verilmezse varsayılan renk kullanılır. */
  brandColor?: string | undefined;
};

export function UiProvider({ children, env = 'default', brandColor }: UiProviderProps) {
  const theme = useMemo(() => createAppTheme(brandColor), [brandColor]);
  return (
    <MantineProvider theme={theme} env={env}>
      {children}
    </MantineProvider>
  );
}
