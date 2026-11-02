import type { LockedLotView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Select, Stack } from '@iplik/ui';
import { useState } from 'react';
import { LotWorkspace } from './LotWorkspace';

/** Parti seçimi; seçilen partinin çalışma alanı her partide sıfırdan kurulur (seçim taşınmaz). */
export function LotPicker({ lots }: { lots: readonly LockedLotView[] }) {
  const { t } = useTranslation();
  const [lotId, setLotId] = useState(lots[0]?.lotId ?? '');
  return (
    <Stack>
      <Select
        label={t('bag.lot')}
        data={lots.map((lot) => ({
          value: lot.lotId,
          label: t('bag.lotOption', {
            lotNo: lot.lotNo,
            grade: lot.qualityGradeCode,
            count: lot.bagCount,
          }),
        }))}
        value={lotId}
        onChange={(value) => {
          setLotId(value ?? lotId);
        }}
        allowDeselect={false}
        searchable
      />
      <LotWorkspace key={lotId} lotId={lotId} />
    </Stack>
  );
}
