import { LOGIN_PATH } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Center, Paper, Stack, Title } from '@iplik/ui';
import { useState } from 'react';
import { ConfirmCodeForm } from '../components/ConfirmCodeForm';
import { RequestCodeForm } from '../components/RequestCodeForm';

type Step = { name: 'request' } | { name: 'confirm'; username: string } | { name: 'done' };

/** Giriş yapmamış kişi için herkese açık sayfa: giriş adıyla SMS kodu iste, kodla yeni şifre belirle. */
export function ForgotPasswordPage() {
  const { t } = useTranslation();
  const [step, setStep] = useState<Step>({ name: 'request' });
  return (
    <Center mih="100vh" p="md">
      <Paper withBorder p="lg" w="100%" maw={420}>
        <Stack>
          <Title order={1} size="h3">
            {t('forgotPassword.title')}
          </Title>
          {step.name === 'request' && (
            <RequestCodeForm
              onSent={(username) => {
                setStep({ name: 'confirm', username });
              }}
            />
          )}
          {step.name === 'confirm' && (
            <ConfirmCodeForm
              username={step.username}
              onDone={() => {
                setStep({ name: 'done' });
              }}
              onResend={() => {
                setStep({ name: 'request' });
              }}
            />
          )}
          {step.name === 'done' && (
            <>
              <Alert color="green">{t('forgotPassword.done')}</Alert>
              <Button component="a" href={LOGIN_PATH}>
                {t('forgotPassword.login')}
              </Button>
            </>
          )}
        </Stack>
      </Paper>
    </Center>
  );
}
