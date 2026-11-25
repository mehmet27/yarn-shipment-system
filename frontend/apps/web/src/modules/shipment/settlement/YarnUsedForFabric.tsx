import { formatDateTime, useTranslation } from '@iplik/i18n';
import { Group, SectionCard, Stack, Text } from '@iplik/ui';
import { QueryState } from '@/modules/shared';
import { formatKgText } from './format';
import { useFabricDraws } from './hooks';

/**
 * Bu sevkiyatın ipliğinden üretilen kumaşlar: kumaş başka bir sevkiyattan girilmiş olsa da hangi iplikten kaç kilo
 * kullanıldığı burada görünür; fire hesabı bu kiloları kullanır.
 */
export function YarnUsedForFabric({ shipmentId }: { shipmentId: string }) {
  const { t } = useTranslation();
  const query = useFabricDraws(shipmentId);
  return (
    <SectionCard title={t('settlement.fabricDraws.title')}>
      <QueryState query={query}>
        {(draws) =>
          draws.length === 0 ? (
            <Text size="sm" c="dimmed">
              {t('settlement.fabricDraws.empty')}
            </Text>
          ) : (
            <Stack gap="xs" data-testid="fabric-draws">
              {draws.map((draw) => (
                <Group
                  key={`${draw.fabricId}|${draw.yarnType}|${draw.lotNo ?? ''}`}
                  justify="space-between"
                  wrap="nowrap"
                  align="flex-start"
                >
                  <Stack gap={0}>
                    <Text size="sm">
                      {t('settlement.fabricDraws.line', {
                        yarn:
                          draw.lotNo === undefined
                            ? draw.yarnType
                            : `${draw.yarnType} (${draw.lotNo})`,
                        fabric: draw.fabricType,
                        shipment: draw.entryShipmentNo,
                      })}
                    </Text>
                    <Text size="xs" c="dimmed">
                      {formatDateTime(draw.receivedAt)}
                    </Text>
                  </Stack>
                  <Text size="sm" ta="right" style={{ whiteSpace: 'nowrap' }}>
                    {formatKgText(draw.netKg)}
                  </Text>
                </Group>
              ))}
            </Stack>
          )
        }
      </QueryState>
    </SectionCard>
  );
}
