import { Box, Stack } from '@iplik/ui';

const NAV_WIDTH_PX = 72;
const ROW_HEIGHT_PX = 8;

/** Önizlemedeki yan menü satırları: nötr; etkin satır biraz koyu. */
export function PreviewNav() {
  return (
    <Stack gap="xs" w={NAV_WIDTH_PX} aria-hidden>
      <Box h={ROW_HEIGHT_PX} w="100%" style={{ background: 'var(--app-line)', borderRadius: 4 }} />
      <Box
        h={ROW_HEIGHT_PX}
        w="70%"
        style={{ background: 'var(--app-line-soft)', borderRadius: 4 }}
      />
      <Box
        h={ROW_HEIGHT_PX}
        w="85%"
        style={{ background: 'var(--app-line-soft)', borderRadius: 4 }}
      />
    </Stack>
  );
}
