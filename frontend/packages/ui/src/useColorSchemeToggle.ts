import { useComputedColorScheme, useMantineColorScheme } from '@mantine/core';

/** Açık/koyu görünüm: geçerli görünümü ve diğerine geçiren işlevi verir. Seçim tarayıcıda saklanır. */
export function useColorSchemeToggle(): { isDark: boolean; toggle: () => void } {
  const { setColorScheme } = useMantineColorScheme();
  const isDark = useComputedColorScheme('light') === 'dark';
  return {
    isDark,
    toggle: () => {
      setColorScheme(isDark ? 'light' : 'dark');
    },
  };
}
