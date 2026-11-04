import { requestPasswordReset } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { Button, Stack, Text, TextInput } from '@iplik/ui';
import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { ResetError } from './ResetError';

/** Giriş adını alır ve SMS kodu ister; yanıt hesabın var olup olmadığını açıklamaz, bu yüzden hep ikinci adıma geçilir. */
export function RequestCodeForm({ onSent }: { onSent: (username: string) => void }) {
  const { t } = useTranslation();
  const [username, setUsername] = useState('');
  const [isInvalid, setIsInvalid] = useState(false);
  const mutation = useMutation({
    mutationFn: (name: string) => requestPasswordReset({ username: name }),
    onSuccess: (_, name) => {
      onSent(name);
    },
  });
  return (
    <form
      noValidate
      onSubmit={(event) => {
        event.preventDefault();
        const name = username.trim();
        setIsInvalid(name === '');
        if (name !== '') mutation.mutate(name);
      }}
    >
      <Stack>
        <Text c="dimmed">{t('forgotPassword.requestIntro')}</Text>
        <TextInput
          label={t('forgotPassword.username')}
          autoComplete="username"
          value={username}
          onChange={(event) => {
            setUsername(event.currentTarget.value);
          }}
          error={isInvalid ? t('forgotPassword.invalid.username') : undefined}
          withAsterisk
        />
        {mutation.isError && <ResetError error={mutation.error} />}
        <Button type="submit" loading={mutation.isPending}>
          {t('forgotPassword.requestSubmit')}
        </Button>
      </Stack>
    </form>
  );
}
