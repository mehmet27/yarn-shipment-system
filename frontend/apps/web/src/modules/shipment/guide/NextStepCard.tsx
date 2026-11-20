import { useTranslation } from '@iplik/i18n';
import { formatKgText } from '../settlement/format';
import { Badge, Box, Group, Stack, Text } from '@iplik/ui';
import { GuideActionButton } from './GuideActionButton';
import type { GuideTask, ShipmentGuideView } from './guideSteps';

/** Renk yalnızca dikkat isteyen durumda: kantar uyuşmazlığı turuncu, iptal kırmızı, biten süreç yeşil. */
const TASK_TONE: Readonly<Partial<Record<GuideTask, string>>> = {
  MISMATCH: 'orange',
  CANCELLED: 'red',
  DONE: 'teal',
};

function toTone(guide: ShipmentGuideView): string | undefined {
  return TASK_TONE[guide.task] ?? (guide.isOwn ? 'brand' : undefined);
}

/**
 * "Sıradaki adım" kutusu: ne yapılacağı, kimin yapacağı ve sıra kullanıcıdaysa basacağı tek düğme. Sıra başkasındaysa
 * kullanıcıya şu an bir şey yapması gerekmediğini söyler.
 */
export function NextStepCard({ guide }: { guide: ShipmentGuideView }) {
  const { t } = useTranslation();
  const tone = toTone(guide);
  const base = `shipment.guide.task.${guide.task}`;
  const kg = guide.remainingKg === undefined ? undefined : formatKgText(guide.remainingKg);
  const who = guide.actor === undefined ? undefined : t(`users.role.${guide.actor}`);
  return (
    <Box
      px="md"
      py="sm"
      data-testid="next-step"
      bg={tone === undefined ? 'var(--app-inset)' : `var(--mantine-color-${tone}-light)`}
      style={{
        border: '1px solid var(--app-line)',
        borderRadius: 'var(--mantine-radius-md)',
        boxShadow:
          tone === undefined ? undefined : `inset 3px 0 0 var(--mantine-color-${tone}-filled)`,
      }}
    >
      <Stack gap="xs" align="flex-start">
        <Group gap="xs">
          <Text size="xs" c="dimmed" fw={600}>
            {t('shipment.nextStep.label')}
          </Text>
          {guide.isOwn && (
            <Badge color="brand" data-testid="your-turn">
              {t('shipment.guide.yourTurn')}
            </Badge>
          )}
        </Group>
        <Text fw={600} c="var(--app-text)">
          {t(`${base}.${guide.isOwn ? 'own' : 'other'}`)}
        </Text>
        <Text size="sm" c="var(--app-text)">
          {guide.isOwn || who === undefined
            ? t(`${base}.hint`, { kg })
            : t('shipment.guide.waiting', { actor: who })}
        </Text>
        {guide.action !== undefined && (
          <GuideActionButton action={guide.action} label={t(`${base}.action`)} />
        )}
      </Stack>
    </Box>
  );
}
