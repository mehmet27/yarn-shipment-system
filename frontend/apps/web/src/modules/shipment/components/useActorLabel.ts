import { useTranslation } from '@iplik/i18n';
import { useCurrentUser } from '@/modules/auth';

/** Backend sistem adımlarını (zamanlayıcı, olay tüketicisi) sıfır UUID ile kaydeder. */
const SYSTEM_ACTOR_ID = '00000000-0000-0000-0000-000000000000';

/**
 * İşlemi yapanı UUID yerine okunur yazar (H-24): sistem, sunucunun çözdüğü ad ya da oturumdaki
 * kullanıcının adı. Ad çözülemediyse kişi adsız gösterilir; UUID gösterilmez.
 */
export function useActorLabel() {
  const { t } = useTranslation();
  const { data: user } = useCurrentUser();
  return (actorId: string, resolvedName?: string) => {
    if (actorId === SYSTEM_ACTOR_ID) return t('common.actor.system');
    if (resolvedName) return resolvedName;
    if (actorId === user?.userId) return user.fullName;
    return t('common.actor.otherUser');
  };
}
