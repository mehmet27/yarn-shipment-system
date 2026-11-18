import { useTranslation } from '@iplik/i18n';
import { Button, FileButton, Icon, SimpleGrid } from '@iplik/ui';
import type { InputHTMLAttributes } from 'react';
import { usePreparedPick } from '../hooks/usePreparedPick';
import { ACCEPT } from '../uploadTypes';

type TestInputProps = InputHTMLAttributes<HTMLInputElement> & { 'data-testid': string };
const CAMERA_INPUT: TestInputProps = { 'data-testid': 'upload-camera-input' };
const FILE_INPUT: TestInputProps = { 'data-testid': 'upload-file-input' };

/**
 * Mobilde "Fotoğraf çek" kamerayı açar; "Dosya seç" galeriyi/dosyaları (PDF dahil) açar. Seçilen fotoğraf cihazda
 * JPEG'e çevrilip küçültülür (telefon HEIC'i ve çok büyük çekimler yüklenemiyordu); PDF olduğu gibi gider.
 */
export function UploadButtons({
  isLoading,
  isDisabled = false,
  onPick,
}: {
  isLoading: boolean;
  isDisabled?: boolean;
  onPick: (file: File | null) => void;
}) {
  const { t } = useTranslation();
  const camera = usePreparedPick(onPick);
  const files = usePreparedPick(onPick);
  const isBusy = isLoading || camera.isPreparing || files.isPreparing;
  return (
    <SimpleGrid cols={{ base: 1, xs: 2 }}>
      <FileButton
        accept="image/*"
        capture="environment"
        inputProps={CAMERA_INPUT}
        resetRef={camera.resetRef}
        onChange={(file) => {
          camera.pick(file);
        }}
      >
        {(props) => (
          <Button
            loading={isBusy}
            disabled={isDisabled}
            leftSection={<Icon name="camera" />}
            {...props}
          >
            {t('evidence.camera')}
          </Button>
        )}
      </FileButton>
      <FileButton
        accept={ACCEPT}
        inputProps={FILE_INPUT}
        resetRef={files.resetRef}
        onChange={(file) => {
          files.pick(file);
        }}
      >
        {(props) => (
          <Button
            variant="default"
            loading={isBusy}
            disabled={isDisabled}
            leftSection={<Icon name="upload" />}
            {...props}
          >
            {t('evidence.file')}
          </Button>
        )}
      </FileButton>
    </SimpleGrid>
  );
}
