import { MantineProvider } from '@mantine/core';
import type { ReactNode } from 'react';
import { theme } from './theme';

export type UiProviderProps = {
  children: ReactNode;
};

export function UiProvider({ children }: UiProviderProps) {
  return <MantineProvider theme={theme}>{children}</MantineProvider>;
}
