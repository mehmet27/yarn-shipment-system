import { useTranslation } from '@iplik/i18n';
import { Button, Group, Stack } from '@iplik/ui';
import { useParams } from 'react-router';
import { QueryState } from '../components/QueryState';
import { ExitSlipBody } from '../components/ExitSlipBody';
import { useShipment } from '../hooks/useShipment';
import { useShipmentBags } from '../hooks/useShipmentBags';

/** Yazdırırken menü ve üst bar gizlenir; yalnızca fiş basılır. */
const PRINT_CSS = `
@media print {
  header, nav, .no-print { display: none !important; }
  main { padding: 0 !important; margin: 0 !important; }
  .exit-slip { border: 0 !important; padding: 0 !important; }
}`;

/** Ambar Çıkış Fişi: sevkiyattaki çuvallar sıra no + kg olarak, toplamıyla yazdırılır. */
export function ExitSlipPage() {
  const { t } = useTranslation();
  const { shipmentId = '' } = useParams();
  const shipmentQuery = useShipment(shipmentId);
  const bagsQuery = useShipmentBags(shipmentId);
  return (
    <QueryState query={shipmentQuery}>
      {(shipment) => (
        <QueryState query={bagsQuery}>
          {(bags) => (
            <Stack>
              <style>{PRINT_CSS}</style>
              <Group className="no-print">
                <Button
                  onClick={() => {
                    window.print();
                  }}
                >
                  {t('shipment.exitSlip.print')}
                </Button>
              </Group>
              <ExitSlipBody shipment={shipment} bags={bags} />
            </Stack>
          )}
        </QueryState>
      )}
    </QueryState>
  );
}
