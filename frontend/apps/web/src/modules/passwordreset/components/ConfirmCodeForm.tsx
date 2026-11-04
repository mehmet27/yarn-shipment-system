import { confirmPasswordReset } from '@iplik/api-client';
import { useTranslation } from '@iplik/i18n';
import { PasswordInput, Stack, Text, TextInput } from '@iplik/ui';
import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { ConfirmActions } from './ConfirmActions';
import { ResetError } from './ResetError';

const CODE_PATTERN = /^\d{6}$/;
const MIN_PASSWORD_LENGTH = 8;

function validate(code: string, password: string) {
  return {
    isCodeInvalid: !CODE_PATTERN.test(code.trim()),
    isPasswordInvalid: password.length < MIN_PASSWORD_LENGTH,
  };
}

/** SMS kodunu ve yeni şifreyi alır; şifre yalnızca bu istekte gider, hiçbir yerde saklanmaz ya da gösterilmez. */
export function ConfirmCodeForm({
  username,
  onDone,
  onResend,
}: {
  username: string;
  onDone: () => void;
  onResend: () => void;
}) {
  const { t } = useTranslation();
  const [code, setCode] = useState('');
  const [password, setPassword] = useState('');
  const [isSubmitted, setIsSubmitted] = useState(false);
  const mutation = useMutation({
    mutationFn: () => confirmPasswordReset({ username, code: code.trim(), newPassword: password }),
    onSuccess: onDone,
  });
  const { isCodeInvalid, isPasswordInvalid } = validate(code, password);
  return (
    <form
      noValidate
      onSubmit={(event) => {
        event.preventDefault();
        setIsSubmitted(true);
        if (!isCodeInvalid && !isPasswordInvalid) mutation.mutate();
      }}
    >
      <Stack>
        <Text c="dimmed">{t('forgotPassword.codeIntro')}</Text>
        <TextInput
          label={t('forgotPassword.code')}
          inputMode="numeric"
          autoComplete="one-time-code"
          maxLength={6}
          value={code}
          onChange={(event) => {
            setCode(event.currentTarget.value);
          }}
          error={isSubmitted && isCodeInvalid ? t('forgotPassword.invalid.code') : undefined}
          withAsterisk
        />
        <PasswordInput
          label={t('forgotPassword.password')}
          description={t('forgotPassword.passwordHint')}
          autoComplete="new-password"
          value={password}
          onChange={(event) => {
            setPassword(event.currentTarget.value);
          }}
          error={
            isSubmitted && isPasswordInvalid ? t('forgotPassword.invalid.password') : undefined
          }
          withAsterisk
        />
        {mutation.isError && <ResetError error={mutation.error} />}
        <ConfirmActions isPending={mutation.isPending} onResend={onResend} />{' '}
      </Stack>
    </form>
  );
}
