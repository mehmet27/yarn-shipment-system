import { useTranslation } from '@iplik/i18n';
import { Box, Group, Stack, Text } from '@iplik/ui';
import type { PreviewScheme } from '@iplik/ui';
import { PreviewSidebar } from './PreviewSidebar';

const BUTTON_RADIUS_PX = 6;
const TAB_LINE = '2px solid';

export type PreviewShellProps = {
  name: string;
  logo: string;
  accent: string;
  scheme: PreviewScheme;
};

/** Uygulamanın küçültülmüş bir görünümü: yan menü, sekmeler ve nötr ana düğme. Süs amaçlıdır; tıklanmaz. */
export function PreviewShell({ name, logo, accent, scheme }: PreviewShellProps) {
  const { t } = useTranslation();
  return (
    <Stack gap={6}>
      <Text size="xs" c="dimmed" fw={500}>
        {t(`branding.preview.${scheme.key}`)}
      </Text>
      <Group
        align="stretch"
        gap={0}
        wrap="nowrap"
        style={{
          background: scheme.surface,
          border: `1px solid ${scheme.line}`,
          borderRadius: 8,
          overflow: 'hidden',
        }}
      >
        <PreviewSidebar name={name} logo={logo} accent={accent} scheme={scheme} />
        <Stack gap="sm" p="sm" miw={0} style={{ flex: 1 }}>
          <Text fw={600} size="sm" style={{ color: scheme.text }}>
            {t('branding.preview.sample')}
          </Text>
          <Group gap="md" wrap="nowrap" style={{ borderBottom: `1px solid ${scheme.line}` }}>
            <Text
              size="xs"
              fw={600}
              pb={4}
              style={{
                color: scheme.text,
                borderBottom: `${TAB_LINE} ${accent}`,
                whiteSpace: 'nowrap',
              }}
            >
              {t('branding.preview.activeTab')}
            </Text>
            <Text size="xs" fw={600} pb={4} truncate style={{ color: scheme.dimmed }}>
              {t('branding.preview.otherTab')}
            </Text>
          </Group>
          <Box
            px="sm"
            py={6}
            fw={600}
            fz="xs"
            style={{
              background: scheme.button,
              color: scheme.buttonText,
              borderRadius: BUTTON_RADIUS_PX,
              alignSelf: 'flex-start',
            }}
          >
            {t('branding.preview.button')}
          </Box>
        </Stack>
      </Group>
    </Stack>
  );
}
