import { useTranslation } from '@iplik/i18n';
import { Button, FileButton, Group, Stack, Text } from '@iplik/ui';
import { LOGO_TYPES, MAX_LOGO_BYTES } from '../brandingDraft';
import { PREVIEW_SCHEMES } from '@iplik/ui';
import { useLogoPicker } from '../useLogoPicker';
import { LogoTile } from './LogoTile';

const BYTES_PER_KB = 1000;

export type LogoFieldProps = { value: string; onChange: (dataUrl: string) => void };

/** Logo seçer ya da kaldırır; tür ve boyut sınırı aşılırsa nedenini söyler. */
export function LogoField({ value, onChange }: LogoFieldProps) {
  const { t } = useTranslation();
  const { error, pick, clearError } = useLogoPicker(onChange);
  return (
    <Stack gap="xs">
      <Text size="sm" fw={500}>
        {t('branding.logo.label')}
      </Text>
      {value === '' ? (
        <Text size="sm" c="dimmed">
          {t('branding.logo.none')}
        </Text>
      ) : (
        <Group gap="sm" wrap="nowrap" align="flex-start">
          {PREVIEW_SCHEMES.map((scheme) => (
            <LogoTile
              key={scheme.key}
              src={value}
              alt={scheme.key === 'light' ? t('branding.logo.alt') : ''}
              caption={t(scheme.key === 'light' ? 'branding.logo.onLight' : 'branding.logo.onDark')}
              background={scheme.surface}
            />
          ))}
        </Group>
      )}
      <Group gap="xs">
        <FileButton
          onChange={(file) => void pick(file)}
          accept={LOGO_TYPES.join(',')}
          inputProps={{ 'aria-label': t('branding.logo.file') }}
        >
          {(props) => (
            <Button variant="default" {...props}>
              {t(value === '' ? 'branding.logo.choose' : 'branding.logo.change')}
            </Button>
          )}
        </FileButton>
        {value !== '' && (
          <Button
            variant="subtle"
            color="red"
            onClick={() => {
              clearError();
              onChange('');
            }}
          >
            {t('branding.logo.remove')}
          </Button>
        )}
      </Group>
      <Text size="xs" c={error === undefined ? 'dimmed' : 'red'}>
        {error === undefined
          ? t('branding.logo.hint', { kb: MAX_LOGO_BYTES / BYTES_PER_KB })
          : t(`branding.logo.errors.${error}`, { kb: MAX_LOGO_BYTES / BYTES_PER_KB })}
      </Text>
    </Stack>
  );
}
