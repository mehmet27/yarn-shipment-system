import type { AuditRecordView } from '@iplik/api-client/audit';
import { useTranslation } from '@iplik/i18n';
import { useCurrentUser } from '@/modules/auth';

/** Backend sistem adımlarını (zamanlayıcı, olay tüketicisi) sıfır UUID ile kaydeder. */
const SYSTEM_ACTOR_ID = '00000000-0000-0000-0000-000000000000';

type Actor = AuditRecordView['actor'];

function isSystem(actor: Actor): boolean {
  return (
    actor.userId === SYSTEM_ACTOR_ID || (actor.userId === undefined && actor.clientId !== undefined)
  );
}

/** Kişinin adı: sunucunun çözdüğü ad soyad, yoksa oturumdaki kullanıcının kendi adı. */
function usePersonName(actor: Actor, names?: ReadonlyMap<string, string>): string | undefined {
  const { data: user } = useCurrentUser();
  if (actor.userId === undefined) return undefined;
  return names?.get(actor.userId) ?? (actor.userId === user?.userId ? user.fullName : undefined);
}

/**
 * İşlemi yapan: kullanıcı kimliği (UUID) ham gösterilmez. Servis hesabı ve sıfır UUID "Sistem",
 * kişi ad soyadıyla yazılır; ad çözülemezse rolüyle ("Depo") görünür. Kimlik yalnızca ipucunda kalır.
 */
export function AuditActor({
  actor,
  names,
}: {
  actor: Actor;
  names?: ReadonlyMap<string, string> | undefined;
}) {
  const { t } = useTranslation();
  const personName = usePersonName(actor, names);
  if (isSystem(actor)) {
    return <span title={actor.clientId ?? actor.userId}>{t('common.actor.system')}</span>;
  }
  const role =
    actor.role === undefined
      ? undefined
      : t(`users.role.${actor.role}`, { defaultValue: actor.role });
  return <span title={actor.userId}>{personName ?? role ?? t('audit.actorUser')}</span>;
}
