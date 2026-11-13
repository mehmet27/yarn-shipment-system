import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Button, Center, Paper, PasswordInput, Stack, Text, TextInput } from '@iplik/ui';
import { useState } from 'react';
import type { SyntheticEvent } from 'react';
import { useLogin } from '../hooks/useLogin';

const KNOWN_CODES = ['AUTHENTICATION_FAILED', 'ACCOUNT_LOCKED'];
const FORM_WIDTH_PX = 380;

function errorKey(error: unknown): string {
  const { code } = toProblem(error);
  return `auth.login.errors.${KNOWN_CODES.includes(code) ? code : 'unknown'}`;
}

/** Kullanıcı adı ve şifre formu; başarılı girişte oturum çerezi gelir, kişi geldiği sayfaya döner. */
export function LoginPage() {
  const { t } = useTranslation();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const { submit, isPending, error } = useLogin();

  const onSubmit = (event: SyntheticEvent) => {
    event.preventDefault();
    submit(username, password, () => {
      setPassword('');
    });
  };

  return (
    <Center h="100vh">
      <Paper withBorder shadow="sm" radius="md" p="xl" w={FORM_WIDTH_PX} maw="92vw">
        <form onSubmit={onSubmit}>
          <Stack>
            <Text fw={700} size="xl" c="brand.7">
              {t('app.name')}
            </Text>
            <Text fw={600}>{t('auth.login.title')}</Text>
            <TextInput
              label={t('auth.login.username')}
              value={username}
              onChange={(event) => {
                setUsername(event.currentTarget.value);
              }}
              autoComplete="username"
              data-autofocus
              required
            />
            <PasswordInput
              label={t('auth.login.password')}
              value={password}
              onChange={(event) => {
                setPassword(event.currentTarget.value);
              }}
              autoComplete="current-password"
              required
            />
            {error !== undefined && <Alert color="red">{t(errorKey(error))}</Alert>}
            <Button type="submit" loading={isPending} disabled={username === '' || password === ''}>
              {t('auth.login.submit')}
            </Button>
          </Stack>
        </form>
      </Paper>
    </Center>
  );
}
