import { CHANGE_PASSWORD_PATH, login, safeReturnTo } from '@iplik/api-client';
import { useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router';
import { clearHelpChat } from '@/modules/assistant';

type Failures = { username: string; count: number };

/** Girişi gönderir; başarılıysa önceki oturumdan kalan önbelleği siler ve kişiyi geldiği sayfaya (ya da şifre değiştirmeye) götürür. */
export function useLogin() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [params] = useSearchParams();
  const [isPending, setIsPending] = useState(false);
  const [error, setError] = useState<unknown>(undefined);
  // Aynı kullanıcı adıyla art arda hatalı deneme sayısı; 3'te "adminden şifre talep et" önerilir.
  const [failures, setFailures] = useState<Failures>({ username: '', count: 0 });

  const submit = (username: string, password: string, onFailure: () => void) => {
    setIsPending(true);
    setError(undefined);
    login({ username: username.trim(), password })
      .then((response) => {
        // Ortak bilgisayarda oturum süresi dolup başka biri giriş yapabilir (çıkış düğmesine basılmadan): önceki
        // kişinin önbellekteki verisi (rapor, tutar, bakiye) ve yardım sohbeti yeni kişiye görünmesin.
        queryClient.clear();
        clearHelpChat();
        void navigate(
          response.passwordChangeRequired
            ? CHANGE_PASSWORD_PATH
            : safeReturnTo(params.get('returnTo')),
          { replace: true },
        );
      })
      .catch((caught: unknown) => {
        setError(caught);
        const next = {
          username: username.trim(),
          count: failures.username === username.trim() ? failures.count + 1 : 1,
        };
        setFailures(next);
        onFailure();
      })
      .finally(() => {
        setIsPending(false);
      });
  };
  return { submit, isPending, error, failures };
}
