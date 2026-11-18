import { Box, Stack, Text } from '@iplik/ui';

const TILE_HEIGHT_PX = 72;
const LOGO_HEIGHT_PX = 40;

export type LogoTileProps = { src: string; alt: string; caption: string; background: string };

/** Logoyu verilen zeminde gösterir; saydam logonun açık ve koyu görünümde nasıl durduğu böyle görülür. */
export function LogoTile({ src, alt, caption, background }: LogoTileProps) {
  return (
    <Stack gap={4} style={{ flex: 1 }} miw={0}>
      <Box
        h={TILE_HEIGHT_PX}
        px="sm"
        style={{
          background,
          border: '1px solid var(--app-line)',
          borderRadius: 8,
          display: 'grid',
          placeItems: 'center',
        }}
      >
        <Box
          component="img"
          src={src}
          alt={alt}
          h={LOGO_HEIGHT_PX}
          maw="100%"
          style={{ objectFit: 'contain' }}
        />
      </Box>
      <Text size="xs" c="dimmed">
        {caption}
      </Text>
    </Stack>
  );
}
