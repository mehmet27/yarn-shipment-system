import { useTranslation } from '@iplik/i18n';
import { Button, Center, Group, Paper, Stack, Title } from '@iplik/ui';
import { Link } from 'react-router';
import { BrandMark } from '@/modules/branding';

const CARD_WIDTH_PX = 420;

export type ErrorPageProps = {
  reason: 'forbidden' | 'notFound' | 'unexpected';
};

/** 403, olmayan adres ve beklenmeyen hata: sayfa zemininde ortalanmış tek kart, tek düğme. */
export function ErrorPage({ reason }: ErrorPageProps) {
  const { t } = useTranslation();
  return (
    <Center mih="100vh" p="md">
      <Paper withBorder radius="md" p="xl" w={CARD_WIDTH_PX} maw="100%" bg="var(--app-surface)">
        <Stack>
          <Group justify="center">
            <BrandMark />
          </Group>
          <Title order={1} ta="center">
            {t(`error.${reason}`)}
          </Title>
          <Button component={Link} to="/" fullWidth>
            {t('error.backHome')}
          </Button>
        </Stack>
      </Paper>
    </Center>
  );
}
