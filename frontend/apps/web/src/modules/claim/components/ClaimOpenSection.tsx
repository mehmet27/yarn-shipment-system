import type { ShipmentClaimsView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Button, Text } from '@iplik/ui';
import { useState } from 'react';
import { OpenClaimForm } from './OpenClaimForm';

/**
 * Yeni itiraz açma bölümü. İtiraz yalnızca girilmiş kumaş için açılır: kumaş girişi yoksa ya da girilen kumaşın
 * tamamına itiraz açıldıysa form yerine nedeni yazılır.
 */
export function ClaimOpenSection({
  shipmentId,
  view,
}: {
  shipmentId: string;
  view: ShipmentClaimsView;
}) {
  const { t } = useTranslation();
  const [isOpening, setOpening] = useState(false);
  if (!(view.allowedActions?.includes('OPEN_CLAIM') ?? false)) return null;
  if (Number(view.receivedKg) <= 0) return hint(t('claim.open.noFabric'));
  if (Number(view.claimableKg) <= 0) return hint(t('claim.open.allClaimed'));
  if (isOpening) {
    return (
      <OpenClaimForm
        shipmentId={shipmentId}
        claimableKg={view.claimableKg}
        onDone={() => {
          setOpening(false);
        }}
      />
    );
  }
  return (
    <Button
      w="fit-content"
      onClick={() => {
        setOpening(true);
      }}
    >
      {t('claim.open.start')}
    </Button>
  );
}

const hint = (text: string) => (
  <Text c="dimmed" size="sm">
    {text}
  </Text>
);
