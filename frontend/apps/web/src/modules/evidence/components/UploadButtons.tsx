import type { EvidenceSource } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Button, FileButton, Group } from '@iplik/ui';
import type { InputHTMLAttributes } from 'react';
import { ACCEPT } from '../uploadTypes';

type TestInputProps = InputHTMLAttributes<HTMLInputElement> & { 'data-testid': string };
const CAMERA_INPUT: TestInputProps = { 'data-testid': 'upload-camera-input' };
const FILE_INPUT: TestInputProps = { 'data-testid': 'upload-file-input' };

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
        inputProps={CAMERA_INPUT}
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
        inputProps={FILE_INPUT}
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
