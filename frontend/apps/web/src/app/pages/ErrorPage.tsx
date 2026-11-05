import { useTranslation } from '@iplik/i18n';
import { Button, Center, Stack, Text } from '@iplik/ui';
import { Link } from 'react-router';

export type ErrorPageProps = {
  reason: 'forbidden' | 'notFound' | 'unexpected';
};

export function ErrorPage({ reason }: ErrorPageProps) {
  const { t } = useTranslation();
  return (
    <Center h="100vh">
      <Stack align="center">
        <Text fw={700} size="xl" c="brand.7">
          {t('app.name')}
        </Text>
        <Text size="lg">{t(`error.${reason}`)}</Text>
        <Button component={Link} to="/">
          {t('error.backHome')}
        </Button>
      </Stack>
    </Center>
  );
}
