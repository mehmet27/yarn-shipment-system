import { useTranslation } from '@iplik/i18n';
import { Button, Paper, Text } from '@iplik/ui';
import { Link } from 'react-router';
import { useShipmentBags } from '../hooks/useShipmentBags';

/**
 * Depo: çıkış tartısını girme eylemi, sıradaki adım şeridinin hemen altında durur. Çuval eklenmeden tartı girilemez
 * (çuval sayısı eşleşmeli); liste yüklenene kadar düğme açık kalır.
 */
export function DispatchBand({ shipmentId }: { shipmentId: string }) {
  const { t } = useTranslation();
  const bags = useShipmentBags(shipmentId, true);
  const isBagsFirst = bags.data?.dispatchBags.length === 0;
  return (
    <Paper withBorder p="md" data-testid="dispatch-band">
      {isBagsFirst ? (
        <Text size="sm" c="dimmed">
          {t('shipment.file.bagsFirst')}
        </Text>
      ) : (
        <Button component={Link} to="cikis" relative="path" w={{ base: '100%', sm: 'auto' }}>
          {t('shipment.file.recordDispatch')}
        </Button>
      )}
    </Paper>
  );
}
