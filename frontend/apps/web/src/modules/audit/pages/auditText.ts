import type { AuditEntity } from '@iplik/api-client/audit';
import { useTranslation } from '@iplik/i18n';

export const SHORT_ID_LENGTH = 8;

const KNOWN_ENTITIES = ['Shipment', 'Evidence', 'Discrepancy'];

/** Kayıt türü Türkçe ("Kanıt 01a1a6d0"); bilinmeyen tür olduğu gibi yazılır. */
export function useEntityLabel(): (entity: AuditEntity | undefined) => string {
  const { t } = useTranslation();
  return (entity) => {
    if (entity === undefined) return '—';
    const type = KNOWN_ENTITIES.includes(entity.type)
      ? t(`audit.entity.${entity.type}`)
      : entity.type;
    return entity.id === undefined ? type : `${type} ${entity.id.slice(0, SHORT_ID_LENGTH)}`;
  };
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
