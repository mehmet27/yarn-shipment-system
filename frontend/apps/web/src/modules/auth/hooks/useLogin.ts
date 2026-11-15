import { CHANGE_PASSWORD_PATH, login, safeReturnTo } from '@iplik/api-client';
import { useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router';
import { CURRENT_USER_QUERY_KEY } from './useCurrentUser';

type Failures = { username: string; count: number };

/** Girişi gönderir; başarılıysa kullanıcı önbelleğini yeniler ve kişiyi geldiği sayfaya (ya da şifre değiştirmeye) götürür. */
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
      .then(async (response) => {
        // Önceki kişinin önbelleği kalmasın: kullanıcı ve izinler yeni oturumla yeniden alınır.
        await queryClient.invalidateQueries({ queryKey: CURRENT_USER_QUERY_KEY });
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
