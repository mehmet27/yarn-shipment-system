import { Group, Title } from '@iplik/ui';
import type { ReactNode } from 'react';

export type AuthCardHeaderProps = {
  title: string;
  /** Kurulumun adı/logosu; kabuk (router) verir, modül marka modülüne bağımlı olmaz. */
  brand?: ReactNode;
};

/** Giriş ve şifre kartlarının üstü: kurulumun adı/logosu ve ortalanmış sayfa başlığı. */
export function AuthCardHeader({ title, brand }: AuthCardHeaderProps) {
  return (
    <>
      {brand === undefined ? null : <Group justify="center">{brand}</Group>}
      <Title order={1} ta="center">
        {title}
      </Title>
    </>
  );
}
