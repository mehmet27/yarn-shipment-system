import { Box, Stack } from '@iplik/ui';

const SHADE_FRAME = 9;
const SHADE_MAIN = 7;
const SHADE_LIGHT = 1;
const NAV_WIDTH_PX = 72;
const MUTED_OPACITY = 0.4;

/** Önizlemedeki yan menü: üst barla aynı düz ton; etkin satır ana renkte. */
export function PreviewNav({ palette }: { palette: readonly string[] }) {
  return (
    <Stack
      gap="xs"
      p="sm"
      w={NAV_WIDTH_PX}
      style={{ background: palette[SHADE_FRAME], flexShrink: 0 }}
      aria-hidden
    >
      <Box h={8} w="100%" style={{ background: palette[SHADE_MAIN], borderRadius: 4 }} />
      <Box
        h={8}
        w="70%"
        style={{ background: palette[SHADE_LIGHT], opacity: MUTED_OPACITY, borderRadius: 4 }}
      />
      <Box
        h={8}
        w="85%"
        style={{ background: palette[SHADE_LIGHT], opacity: MUTED_OPACITY, borderRadius: 4 }}
      />
    </Stack>
  );
}
