import { Button, FileButton } from '@iplik/ui';
import type { InputHTMLAttributes } from 'react';

export type CameraButtonProps = {
  label: string;
  testId: string;
  isLoading?: boolean;
  variant?: 'filled' | 'default';
  onPick: (file: File) => void;
};

type CameraInputProps = InputHTMLAttributes<HTMLInputElement> & { 'data-testid': string };

/** Yalnızca kamera açar (`capture`); galeri seçeneği bilerek yoktur. */
export function CameraButton({
  label,
  testId,
  isLoading = false,
  variant = 'filled',
  onPick,
}: CameraButtonProps) {
  const inputProps: CameraInputProps = { capture: 'environment', 'data-testid': testId };
  return (
    <FileButton
      accept="image/*"
      inputProps={inputProps}
      onChange={(file) => {
        if (file !== null) onPick(file);
      }}
    >
      {(props) => (
        <Button variant={variant} loading={isLoading} {...props}>
          {label}
        </Button>
      )}
    </FileButton>
  );
}
