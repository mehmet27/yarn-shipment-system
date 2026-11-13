import { toProblem } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Alert, Center, Paper, PasswordInput, Stack, Text } from '@iplik/ui';
import { useState } from 'react';
import type { SyntheticEvent } from 'react';
import { ChangePasswordActions } from '../components/ChangePasswordActions';
import { useChangeOwnPassword } from '../hooks/useChangeOwnPassword';
import { useCurrentUser } from '../hooks/useCurrentUser';

const KNOWN_CODES = ['INVALID_CURRENT_PASSWORD', 'WEAK_PASSWORD'];
const MIN_PASSWORD_LENGTH = 8;
const FORM_WIDTH_PX = 420;

function field(set: (value: string) => void) {
  return {
    onChange: (event: SyntheticEvent<HTMLInputElement>) => {
      set(event.currentTarget.value);
    },
    required: true,
  };
}

function errorKey(error: unknown): string {
  const { code } = toProblem(error);
  return `auth.changePassword.errors.${KNOWN_CODES.includes(code) ? code : 'unknown'}`;
}

/**
 * Kişi kendi şifresini değiştirir. Geçici şifreyle girenler (`passwordChangeRequired`) başka sayfaya geçemez, bu
 * yüzden "Vazgeç" yalnızca zorunlu olmayan durumda görünür.
 */
export function ChangePasswordPage() {
  const { t } = useTranslation();
  const { data: user } = useCurrentUser();
  const [current, setCurrent] = useState('');
  const [next, setNext] = useState('');
  const [repeat, setRepeat] = useState('');
  const { submit, isPending, error } = useChangeOwnPassword();
  const isMismatch = repeat !== '' && next !== repeat;
  const canSubmit = current !== '' && next.length >= MIN_PASSWORD_LENGTH && next === repeat;
  return (
    <Center h="100vh">
      <Paper withBorder shadow="sm" radius="md" p="xl" w={FORM_WIDTH_PX} maw="92vw">
        <form
          onSubmit={(event) => {
            event.preventDefault();
            submit(current, next);
          }}
        >
          <Stack>
            <Text fw={700} size="xl" c="brand.7">
              {t('auth.changePassword.title')}
            </Text>
            <Text size="sm">{t('auth.changePassword.intro')}</Text>
            <PasswordInput
              label={t('auth.changePassword.current')}
              value={current}
              autoComplete="current-password"
              {...field(setCurrent)}
            />
            <PasswordInput
              label={t('auth.changePassword.new')}
              description={t('auth.changePassword.hint')}
              value={next}
              autoComplete="new-password"
              {...field(setNext)}
            />
            <PasswordInput
              label={t('auth.changePassword.repeat')}
              value={repeat}
              error={isMismatch ? t('auth.changePassword.mismatch') : undefined}
              autoComplete="new-password"
              {...field(setRepeat)}
            />
            {error !== undefined && <Alert color="red">{t(errorKey(error))}</Alert>}
            <ChangePasswordActions
              isCancelHidden={user?.passwordChangeRequired === true}
              isPending={isPending}
              canSubmit={canSubmit}
            />
          </Stack>
        </form>
      </Paper>
    </Center>
  );
}
