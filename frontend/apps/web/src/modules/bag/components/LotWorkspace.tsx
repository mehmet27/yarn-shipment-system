import { useTranslation } from '@iplik/i18n';
import { Paper, Stack, Title } from '@iplik/ui';
import { useState } from 'react';
import { QueryState } from '@/modules/shipment';
import { useLotBags } from '../hooks/useLotBags';
import { CreateBagsForm } from './CreateBagsForm';
import { LabelPreviewModal } from './LabelPreviewModal';
import { LotBagsPanel } from './LotBagsPanel';

/** Bir partinin çuval üretimi, çuval listesi ve etiket baskısı. Parti değişince sayfa bu bileşeni yeniden kurar. */
export function LotWorkspace({ lotId }: { lotId: string }) {
  const { t } = useTranslation();
  const query = useLotBags(lotId);
  const [selected, setSelected] = useState<ReadonlySet<string>>(new Set());
  const [pdf, setPdf] = useState<Blob>();
  return (
    <Stack>
      <Paper withBorder p="md">
        <CreateBagsForm
          lotId={lotId}
          onCreated={(keys) => {
            setSelected(new Set(keys));
          }}
        />
      </Paper>
      <Title order={2} size="h4">
        {t('bag.bags')}
      </Title>
      <QueryState query={query}>
        {(bags) => (
          <LotBagsPanel
            bags={bags}
            selected={selected}
            onSelect={setSelected}
            onPrinted={(file) => {
              setSelected(new Set());
              setPdf(file);
            }}
          />
        )}
      </QueryState>
      <LabelPreviewModal
        pdf={pdf}
        onClose={() => {
          setPdf(undefined);
        }}
      />
    </Stack>
  );
}
