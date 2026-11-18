import { Box, Group, Stack, Text } from '@iplik/ui';
import { PreviewNav } from './PreviewNav';

const LOGO_HEIGHT_PX = 24;
const MARK_SIZE_PX = 20;
const NAME_MAX_WIDTH_PX = 96;

export type PreviewSidebarProps = { name: string; logo: string; accent: string | undefined };

/** Önizlemedeki açık gri yan menü: firma işareti/logosu, adı ve nötr satırlar. */
export function PreviewSidebar({ name, logo, accent }: PreviewSidebarProps) {
  return (
    <Stack
      gap="md"
      p="sm"
      style={{
        background: 'var(--app-frame-bg)',
        borderRight: '1px solid var(--app-line)',
        flexShrink: 0,
      }}
    >
      <Group gap="xs" wrap="nowrap">
        {logo === '' ? (
          <Box
            aria-hidden
            w={MARK_SIZE_PX}
            h={MARK_SIZE_PX}
            style={{ background: accent, borderRadius: 6, flexShrink: 0 }}
          />
        ) : (
          <Box
            component="img"
            src={logo}
            alt=""
            h={LOGO_HEIGHT_PX}
            style={{ objectFit: 'contain' }}
          />
        )}
        <Text fw={600} size="sm" truncate maw={NAME_MAX_WIDTH_PX}>
          {name}
        </Text>
      </Group>
      <PreviewNav />
    </Stack>
  );
}
