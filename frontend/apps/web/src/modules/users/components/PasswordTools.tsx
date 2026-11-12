import { useTranslation } from '@iplik/i18n';
import { Button, Group } from '@iplik/ui';
import { useEffect, useState } from 'react';
import { generatePassword } from '../generatePassword';

export type PasswordToolsProps = { password: string; onGenerate: (password: string) => void };

/** Yazılan geçici şifreyi rastgele üretir ya da panoya kopyalar; şifre değişince "Kopyalandı" sıfırlanır. */
export function PasswordTools({ password, onGenerate }: PasswordToolsProps) {
  const { t } = useTranslation();
  const [isCopied, setIsCopied] = useState(false);
  useEffect(() => {
    setIsCopied(false);
  }, [password]);
  return (
    <Group gap="xs">
      <Button
        variant="default"
        onClick={() => {
          onGenerate(generatePassword());
        }}
      >
        {t('users.setPassword.generate')}
      </Button>
      <Button
        variant="default"
        disabled={password === ''}
        onClick={() => {
          void navigator.clipboard.writeText(password).then(() => {
            setIsCopied(true);
          });
        }}
      >
        {t(isCopied ? 'users.setPassword.copied' : 'users.setPassword.copy')}
      </Button>
    </Group>
  );
}
