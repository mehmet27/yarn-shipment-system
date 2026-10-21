import { MantineProvider } from '@mantine/core';
import type { ReactNode } from 'react';
import { theme } from './theme';

export type UiProviderProps = {
  children: ReactNode;
  /** Testlerde geçiş animasyonlarını ve portal'ları kapatır. */
  env?: 'default' | 'test';
};

export function UiProvider({ children, env = 'default' }: UiProviderProps) {
  return (
    <MantineProvider theme={theme} env={env}>
      {children}
    </MantineProvider>
  );
}
