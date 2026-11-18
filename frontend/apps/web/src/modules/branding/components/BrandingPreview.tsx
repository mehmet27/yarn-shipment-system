import { useTranslation } from '@iplik/i18n';
import {
  Box,
  createBrandPalette,
  DEFAULT_BRAND_COLOR,
  Group,
  isValidHexColor,
  Paper,
  Stack,
  Text,
} from '@iplik/ui';
import type { BrandingDraft } from '../brandingDraft';
import { PreviewSidebar } from './PreviewSidebar';

const SHADE_ACCENT = 7;

export type BrandingPreviewProps = { draft: BrandingDraft };

/** Yeni kabuğu gösterir: açık gri yan menü, beyaz üst çubuk, nötr ana düğme; kurulum rengi yalnızca vurguda (işaret, bağlantı). Uygulamanın temasını değiştirmez. */
export function BrandingPreview({ draft }: BrandingPreviewProps) {
  const { t } = useTranslation();
  const palette = createBrandPalette(
    isValidHexColor(draft.primaryColor) ? draft.primaryColor : DEFAULT_BRAND_COLOR,
  );
  const accent = palette[SHADE_ACCENT];
  const name = draft.companyName.trim() === '' ? t('app.name') : draft.companyName.trim();
  return (
    <Paper
      withBorder
      radius="md"
      style={{ overflow: 'hidden', borderColor: 'var(--app-line)' }}
      bg="var(--app-surface)"
      aria-label={t('branding.preview.title')}
    >
      <Group align="stretch" gap={0} wrap="nowrap">
        <PreviewSidebar name={name} logo={draft.logoDataUrl} accent={accent} />
        <Stack gap={0} miw={0} style={{ flex: 1 }}>
          <Box
            h={32}
            bg="var(--app-surface)"
            style={{ borderBottom: '1px solid var(--app-line)' }}
            aria-hidden
          />
          <Stack p="md" gap="sm">
            <Text fw={600} style={{ color: accent }}>
              {t('branding.preview.sample')}
            </Text>
            <Box
              px="md"
              py="xs"
              fw={600}
              bg="var(--mantine-color-ink-filled)"
              c="var(--mantine-primary-color-contrast)"
              style={{ borderRadius: 6, alignSelf: 'flex-start' }}
            >
              {t('branding.preview.button')}
            </Box>
          </Stack>
        </Stack>
      </Group>
    </Paper>
  );
}
