import { createTheme } from '@mantine/core';

/** Saha ekranları güneş altında da okunmalı: yüksek kontrast, büyük dokunma hedefleri (en az 48 px). */
export const theme = createTheme({
  primaryColor: 'indigo',
  defaultRadius: 'md',
  fontFamily: 'system-ui, -apple-system, "Segoe UI", Roboto, sans-serif',
  components: {
    Button: { defaultProps: { size: 'md' } },
  },
});
