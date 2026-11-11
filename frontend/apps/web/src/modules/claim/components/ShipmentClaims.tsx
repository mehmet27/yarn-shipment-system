import type { ShipmentClaimsView } from '@iplik/api-client';
import { formatDateTime, useTranslation } from '@iplik/i18n';
import { Button, Stack, Text } from '@iplik/ui';
import { useState } from 'react';
import { ClaimCard } from './ClaimCard';
import { OpenClaimForm } from './OpenClaimForm';

/** Teslimde okutulmuş ve henüz itiraz edilmemiş anahtarlar. */
function unclaimedKeys(view: ShipmentClaimsView): string[] {
  const claimed = new Set(view.claims.flatMap((claim) => claim.bagKeys));
  return view.deliveredKeys.filter((key) => !claimed.has(key));
}

/** İtiraz süresi, yeni itiraz formu ve açılmış itirazlar. */
export function ShipmentClaims({
  shipmentId,
  view,
}: {
  shipmentId: string;
  view: ShipmentClaimsView;
}) {
  const { t } = useTranslation();
  const [isOpening, setOpening] = useState(false);
  const canOpen = view.allowedActions?.includes('OPEN_CLAIM') ?? false;
  const deadline = formatDateTime(view.deadlineAt);
  return (
    <Stack>
      <Text c="dimmed">
        {t(view.windowOpen ? 'claim.window.open' : 'claim.window.closed', { deadline })}
      </Text>
      {canOpen && !isOpening && (
        <Button
          w="fit-content"
          onClick={() => {
            setOpening(true);
          }}
        >
          {t('claim.open.start')}
        </Button>
      )}
      {canOpen && isOpening && (
        <OpenClaimForm
          shipmentId={shipmentId}
          bagKeys={unclaimedKeys(view)}
          onDone={() => {
            setOpening(false);
          }}
        />
      )}
      {view.claims.length === 0 && <Text c="dimmed">{t('claim.empty')}</Text>}
      {view.claims.map((claim) => (
        <ClaimCard key={claim.id} claim={claim} />
      ))}
    </Stack>
  );
}
