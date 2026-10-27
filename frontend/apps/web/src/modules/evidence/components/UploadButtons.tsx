import type { EvidenceSource } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Button, FileButton, Group } from '@iplik/ui';
import { ACCEPT } from '../uploadTypes';

/** Mobilde "Fotoğraf çek" kamerayı açar; "Dosya seç" galeriyi/dosyaları (PDF dahil) açar. */
export function UploadButtons({
  isLoading,
  onPick,
}: {
  isLoading: boolean;
  onPick: (file: File | null, source: EvidenceSource) => void;
}) {
  const { t } = useTranslation();
  return (
    <Group grow>
      <FileButton
        accept="image/*"
        capture="environment"
        onChange={(file) => {
          onPick(file, 'CAMERA');
        }}
      >
        {(props) => (
          <Button size="lg" loading={isLoading} {...props}>
            {t('evidence.camera')}
          </Button>
        )}
      </FileButton>
      <FileButton
        accept={ACCEPT}
        onChange={(file) => {
          onPick(file, 'GALLERY');
        }}
      >
        {(props) => (
          <Button size="lg" variant="default" loading={isLoading} {...props}>
            {t('evidence.file')}
          </Button>
        )}
      </FileButton>
    </Group>
  );
}
