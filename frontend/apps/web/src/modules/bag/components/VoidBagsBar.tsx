import { useTranslation } from '@iplik/i18n';
import { Button, Text } from '@iplik/ui';
import { useState } from 'react';
import { VoidReasonModal } from './VoidReasonModal';

export type VoidBagsBarProps = {
  /** Seçilenlerden yalnızca depodaki (iptal edilebilen) çuvalların anahtarları. */
  uniqueKeys: string[];
  /** Seçimde iptal edilemeyen (sevkiyatta ya da yolda) çuval var mı? */
  hasBlocked: boolean;
  onVoided: () => void;
};

/** Seçili, depodaki çuvalları iptal eder; geri alınamadığı için gerekçe ve onay penceresi sorar. */
export function VoidBagsBar({ uniqueKeys, hasBlocked, onVoided }: VoidBagsBarProps) {
  const { t } = useTranslation();
  const [isOpen, setIsOpen] = useState(false);
  return (
    <>
      <Button
        color="red"
        variant="light"
        disabled={uniqueKeys.length === 0 || hasBlocked}
        onClick={() => {
          setIsOpen(true);
        }}
      >
        {t('bag.void.open')}
      </Button>
      {hasBlocked && (
        <Text size="sm" c="dimmed">
          {t('bag.void.blocked')}
        </Text>
      )}
      <VoidReasonModal
        isOpen={isOpen}
        uniqueKeys={uniqueKeys}
        onClose={() => {
          setIsOpen(false);
        }}
        onVoided={onVoided}
      />
    </>
  );
}
