import { Group, Title } from '@iplik/ui';
import { BrandMark } from '@/modules/branding';

/** Giriş ve şifre kartlarının üstü: kurulumun adı/logosu ve ortalanmış sayfa başlığı. */
export function AuthCardHeader({ title }: { title: string }) {
  return (
    <>
      <Group justify="center">
        <BrandMark />
      </Group>
      <Title order={1} ta="center">
        {title}
      </Title>
    </>
  );
}
