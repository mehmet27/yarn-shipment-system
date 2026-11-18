import { useTranslation } from '@iplik/i18n';
import { Box, Group, Stack, Text } from '@iplik/ui';
import type { PreviewScheme } from '@iplik/ui';

const LOGO_HEIGHT_PX = 22;
const MARK_SIZE_PX = 20;
const SIDEBAR_WIDTH_PX = 132;
const ROW_RADIUS_PX = 6;

export type PreviewSidebarProps = {
  name: string;
  logo: string;
  accent: string;
  scheme: PreviewScheme;
};

/** Önizlemedeki yan menü: firma işareti/logosu, adı; etkin satırın solunda kurulum renginde çizgi. */
export function PreviewSidebar({ name, logo, accent, scheme }: PreviewSidebarProps) {
  const { t } = useTranslation();
  return (
    <Stack
      gap="sm"
      p="sm"
      w={SIDEBAR_WIDTH_PX}
      style={{
        background: scheme.frame,
        borderRight: `1px solid ${scheme.line}`,
        flexShrink: 0,
      }}
    >
      <Group gap={6} wrap="nowrap">
        {logo === '' ? (
          <Box
            w={MARK_SIZE_PX}
            h={MARK_SIZE_PX}
            style={{ background: accent, borderRadius: ROW_RADIUS_PX, flexShrink: 0 }}
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
        <Text fw={600} size="xs" truncate style={{ color: scheme.text }}>
          {name}
        </Text>
      </Group>
      <Stack gap={4}>
        <Text
          size="xs"
          fw={500}
          px="xs"
          py={4}
          style={{
            color: scheme.text,
            background: scheme.activeRow,
            borderRadius: ROW_RADIUS_PX,
            boxShadow: `inset 3px 0 0 ${accent}`,
          }}
        >
          {t('branding.preview.activeMenu')}
        </Text>
        <Text size="xs" px="xs" py={4} style={{ color: scheme.dimmed }}>
          {t('branding.preview.otherMenu')}
        </Text>
      </Stack>
    </Stack>
  );
}
