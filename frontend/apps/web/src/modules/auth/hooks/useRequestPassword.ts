import { requestPassword } from '@iplik/api-client';
import { useMutation } from '@tanstack/react-query';

/** Hesap sahibi adminden şifre ister; cevap her durumda başarılıdır (hesabın varlığı sızmaz). */
export function useRequestPassword() {
  return useMutation({
    mutationFn: (username: string) => requestPassword({ username: username.trim() }),
  });
}
