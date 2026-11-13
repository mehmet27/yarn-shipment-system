import { useTranslation } from '@iplik/i18n';
import { Button, FileButton, Group, Text } from '@iplik/ui';
import { useState } from 'react';
import { isLogoType, MAX_LOGO_BYTES, readFileAsDataUrl } from '../logo';

export type LogoControlProps = {
  logo: string | null;
  onChange: (logo: string | null) => void;
};

/** İsteğe bağlı rapor logosu: PNG/JPEG, en çok 200 KB. */
export function LogoControl({ logo, onChange }: LogoControlProps) {
  const { t } = useTranslation();
  const [hasError, setHasError] = useState(false);
  async function pick(file: File | null) {
    if (file === null) return;
    if (!isLogoType(file.type) || file.size > MAX_LOGO_BYTES) {
      setHasError(true);
      return;
    }
    setHasError(false);
    onChange(await readFileAsDataUrl(file));
  }
  return (
    <Group gap="xs">
      <FileButton
        accept="image/png,image/jpeg"
        inputProps={{ 'aria-label': t('report.logo.file') }}
        onChange={(file) => void pick(file)}
      >
        {(props) => (
          <Button variant="subtle" size="md" {...props}>
            {logo === null ? t('report.logo.add') : t('report.logo.change')}
          </Button>
        )}
      </FileButton>
      {logo !== null && (
        <Button
          variant="subtle"
          color="red"
          size="compact-sm"
          onClick={() => {
            setHasError(false);
            onChange(null);
          }}
        >
          {t('report.logo.remove')}
        </Button>
      )}
      {hasError && (
        <Text size="sm" c="red" role="alert">
          {t('report.logo.invalid')}
        </Text>
      )}
    </Group>
  );
}
