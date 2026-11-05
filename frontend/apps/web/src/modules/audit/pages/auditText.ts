import type { AuditEntity } from '@iplik/api-client/audit';
import { useTranslation } from '@iplik/i18n';

export const SHORT_ID_LENGTH = 8;

export function entityLabel(entity: AuditEntity | undefined): string {
  if (entity === undefined) return '—';
  return entity.id === undefined
    ? entity.type
    : `${entity.type} ${entity.id.slice(0, SHORT_ID_LENGTH)}`;
}

/** "com.yarnshipment.discrepancy.discrepancy-opened.v1" biçimindeki olay adı. */
const EVENT_PATTERN = /^com\.yarnshipment\.[a-z-]+\.([a-z-]+)\.v\d+$/;

/** Olay adını Türkçe etikete çevirir; tanınmayan işlem adı olduğu gibi gösterilir. */
export function useActionLabel(): (action: string) => string {
  const { t } = useTranslation();
  return (action) => {
    const slug = EVENT_PATTERN.exec(action)?.[1];
    return slug === undefined
      ? action
      : t(`audit.actionName.${slug}`, { defaultValue: slug.replaceAll('-', ' ') });
  };
}
