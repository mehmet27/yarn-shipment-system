import { Group, Paper } from '@mantine/core';
import { useElementSize } from '@mantine/hooks';
import type { ReactNode } from 'react';
import { useEffect } from 'react';

export type ActionBarProps = {
  children: ReactNode;
};

/** Sabit Yardım düğmesi bu değişken kadar yukarı kayar; çubuktaki düğmeleri örtmez. */
const HEIGHT_VARIABLE = '--app-action-bar-height';
const PADDING_PX = 24;

/** Ana eylemleri ekranın altında sabit tutar; telefonda tek elle erişim için. */
export function ActionBar({ children }: ActionBarProps) {
  const { ref, height } = useElementSize();
  useEffect(() => {
    const root = document.documentElement;
    root.style.setProperty(HEIGHT_VARIABLE, `${String(Math.ceil(height) + PADDING_PX)}px`);
    return () => {
      root.style.removeProperty(HEIGHT_VARIABLE);
    };
  }, [height]);
  return (
    <Paper
      ref={ref}
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
