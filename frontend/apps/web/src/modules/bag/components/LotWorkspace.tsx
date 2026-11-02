import type { LabeledBagView } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Paper, Stack, Title } from '@iplik/ui';
import { useState } from 'react';
import { QueryState } from '@/modules/shipment';
import { useLotBags } from '../hooks/useLotBags';
import { CreateBagsForm } from './CreateBagsForm';
import { LabelPreviewModal } from './LabelPreviewModal';
import { isPrintable, LotBagTable } from './LotBagTable';
import { PrintLabelsBar } from './PrintLabelsBar';

/** Bir partinin çuval üretimi, çuval listesi ve etiket baskısı. Parti değişince sayfa bu bileşeni yeniden kurar. */
export function LotWorkspace({ lotId }: { lotId: string }) {
  const { t } = useTranslation();
  const query = useLotBags(lotId);
  const [selected, setSelected] = useState<ReadonlySet<string>>(new Set());
  const [pdf, setPdf] = useState<Blob>();
  const toggle = (key: string) => {
    const next = new Set(selected);
    if (!next.delete(key)) next.add(key);
    setSelected(next);
  };
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
          <Stack>
            <PrintLabelsBar
              uniqueKeys={[...selected]}
              hasReprint={hasReprint(bags, selected)}
              onPrinted={(file) => {
                setSelected(new Set());
                setPdf(file);
              }}
            />
            <LotBagTable
              bags={bags}
              selected={selected}
              onToggle={toggle}
              onToggleAll={(keys) => {
                setSelected(new Set(keys));
              }}
            />
          </Stack>
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

function hasReprint(bags: readonly LabeledBagView[], selected: ReadonlySet<string>): boolean {
  return bags.some(
    (item) => isPrintable(item) && item.printCount > 0 && selected.has(item.bag.uniqueKey),
  );
}
