import { changeOwnPassword } from '@iplik/api-client';
import { useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router';
import { CURRENT_USER_QUERY_KEY } from './useCurrentUser';

/** Kişinin kendi şifresini değiştirir; başarılıysa kullanıcı yeniden alınır ve ana sayfaya dönülür. */
export function useChangeOwnPassword() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [isPending, setIsPending] = useState(false);
  const [error, setError] = useState<unknown>(undefined);

  const submit = (currentPassword: string, newPassword: string) => {
    setIsPending(true);
    setError(undefined);
    changeOwnPassword({ currentPassword, newPassword })
      .then(async () => {
        await queryClient.invalidateQueries({ queryKey: CURRENT_USER_QUERY_KEY });
        void navigate('/', { replace: true });
      })
      .catch((caught: unknown) => {
        setError(caught);
      })
      .finally(() => {
        setIsPending(false);
      });
  };
  return { submit, isPending, error };
}
