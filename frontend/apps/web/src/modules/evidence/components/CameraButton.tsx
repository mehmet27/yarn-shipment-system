import { Button, FileButton } from '@iplik/ui';
import type { InputHTMLAttributes } from 'react';
import { useRef, useState } from 'react';
import { preparePhoto } from '../preparePhoto';

export type CameraButtonProps = {
  label: string;
  testId: string;
  isLoading?: boolean;
  variant?: 'filled' | 'default';
  /** Verilirse düğmenin soluna konan ikon (dekoratif). */
  icon?: React.ReactNode;
  onPick: (file: File) => void;
};

type CameraInputProps = InputHTMLAttributes<HTMLInputElement> & { 'data-testid': string };

/**
 * Yalnızca kamera açar (`capture`); galeri seçeneği bilerek yoktur. Çekilen fotoğraf cihazda JPEG'e çevrilir ve
 * küçültülür (HEIC, 50 MP+ dosyalar), sonra verilir.
 */
export function CameraButton({
  label,
  testId,
  isLoading = false,
  variant = 'filled',
  icon,
  onPick,
}: CameraButtonProps) {
  const [isPreparing, setPreparing] = useState(false);
  const resetRef = useRef<() => void>(null);
  const inputProps: CameraInputProps = { capture: 'environment', 'data-testid': testId };
  return (
    <FileButton
      accept="image/*"
      inputProps={inputProps}
      resetRef={resetRef}
      onChange={(file) => {
        if (file === null) return;
        setPreparing(true);
        void preparePhoto(file)
          .then(onPick)
          .finally(() => {
            setPreparing(false);
            // Aynı dosya yeniden seçilince de change olayı gelsin.
            resetRef.current?.();
          });
      }}
    >
      {(props) => (
        <Button
          variant={variant}
          loading={isLoading || isPreparing}
          {...(icon === undefined ? {} : { leftSection: icon })}
          {...props}
        >
          {label}
        </Button>
      )}
    </FileButton>
  );
}
