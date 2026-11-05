import type { AuditRecordView } from '@iplik/api-client/audit';
import { useTranslation } from '@iplik/i18n';
import { useCurrentUser } from '@/modules/auth';

/** Backend sistem adımlarını (zamanlayıcı, olay tüketicisi) sıfır UUID ile kaydeder. */
const SYSTEM_ACTOR_ID = '00000000-0000-0000-0000-000000000000';

/**
 * İşlemi yapan: kullanıcı kimliği (UUID) ham gösterilmez. Servis hesabı ve sıfır UUID "Sistem",
 * oturumdaki kullanıcı kendi adıyla, diğer kişiler rolleriyle ("Depo") yazılır; kimlik ipucunda kalır.
 */
export function AuditActor({ actor }: { actor: AuditRecordView['actor'] }) {
  const { t } = useTranslation();
  const { data: user } = useCurrentUser();
  const isSystem =
    actor.userId === SYSTEM_ACTOR_ID ||
    (actor.userId === undefined && actor.clientId !== undefined);
  if (isSystem) {
    return <span title={actor.clientId ?? actor.userId}>{t('common.actor.system')}</span>;
  }
  const ownName = user !== undefined && actor.userId === user.userId ? user.fullName : undefined;
  const role =
    actor.role === undefined
      ? undefined
      : t(`users.role.${actor.role}`, { defaultValue: actor.role });
  return <span title={actor.userId}>{ownName ?? role ?? t('audit.actorUser')}</span>;
}
