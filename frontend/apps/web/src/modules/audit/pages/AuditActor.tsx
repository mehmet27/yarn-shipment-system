import type { AuditRecordView } from '@iplik/api-client/audit';
import { useTranslation } from '@iplik/i18n';
import { SHORT_ID_LENGTH } from './auditText';

/** Yapan: kullanıcı kimliği (UUID) ham gösterilmez; servis hesabı "Sistem", kişi rolü ve kısa kimlikle yazılır. */
export function AuditActor({ actor }: { actor: AuditRecordView['actor'] }) {
  const { t } = useTranslation();
  if (actor.userId === undefined && actor.clientId !== undefined) {
    return <span title={actor.clientId}>{t('common.actor.system')}</span>;
  }
  const short = actor.userId?.slice(0, SHORT_ID_LENGTH);
  const role =
    actor.role === undefined
      ? undefined
      : t(`users.role.${actor.role}`, { defaultValue: actor.role });
  return (
    <span title={actor.userId}>
      {role ?? '—'}
      {short === undefined ? '' : ` · ${short}`}
    </span>
  );
}
