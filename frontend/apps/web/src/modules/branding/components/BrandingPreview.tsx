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

const SHADE_DARK = 9;
const SHADE_MAIN = 7;
const SHADE_LIGHT = 1;
const LOGO_HEIGHT_PX = 28;

export type BrandingPreviewProps = { draft: BrandingDraft };

/** Seçilen renklerin uygulamada nasıl görüneceğini canlı gösterir; uygulamanın kendi temasını değiştirmez. */
export function BrandingPreview({ draft }: BrandingPreviewProps) {
  const { t } = useTranslation();
  const palette = createBrandPalette(
    isValidHexColor(draft.primaryColor) ? draft.primaryColor : DEFAULT_BRAND_COLOR,
  );
  const gradient = `linear-gradient(120deg, ${palette[SHADE_DARK]}, ${palette[SHADE_MAIN]})`;
  const name = draft.companyName.trim() === '' ? t('app.name') : draft.companyName.trim();
  return (
    <Paper
      withBorder
      radius="md"
      style={{ overflow: 'hidden' }}
      aria-label={t('branding.preview.title')}
    >
      <Group px="md" py="sm" c="white" style={{ background: gradient }} gap="xs" wrap="nowrap">
        {draft.logoDataUrl === '' ? null : (
          <Box
            component="img"
            src={draft.logoDataUrl}
            alt=""
            h={LOGO_HEIGHT_PX}
            style={{ objectFit: 'contain' }}
          />
        )}
        <Text fw={700} truncate>
          {name}
        </Text>
      </Group>
      <Stack p="md" gap="sm">
        <Text fw={600}>{t('branding.preview.sample')}</Text>
        <Box h={8} style={{ background: palette[SHADE_LIGHT], borderRadius: 4 }}>
          <Box h={8} w="60%" style={{ background: palette[SHADE_MAIN], borderRadius: 4 }} />
        </Box>
        <Box
          px="md"
          py="xs"
          c="white"
          fw={600}
          style={{ background: palette[SHADE_MAIN], borderRadius: 6, alignSelf: 'flex-start' }}
        >
          {t('branding.preview.button')}
        </Box>
      </Stack>
    </Paper>
  );
}
