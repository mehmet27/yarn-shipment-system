import type { AuditEntity } from '@iplik/api-client/audit';
import { useTranslation } from '@iplik/i18n';

const KNOWN_ENTITIES = ['Shipment', 'Evidence', 'Discrepancy', 'CentralStock'];

/** Kayıt türü Türkçe ("Kanıt"); kimlik ham gösterilmez, ipucunda kalır. Bilinmeyen tür olduğu gibi yazılır. */
export function useEntityLabel(): (entity: AuditEntity | undefined) => string {
  const { t } = useTranslation();
  return (entity) => {
    if (entity === undefined) return '—';
    return KNOWN_ENTITIES.includes(entity.type) ? t(`audit.entity.${entity.type}`) : entity.type;
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

/** Olayı yazan servisin adı Türkçe ("shipment-service" → "Sevkiyat"); bilinmeyen servis olduğu gibi yazılır. */
export function useServiceLabel(): (service: string) => string {
  const { t } = useTranslation();
  return (service) => t(`audit.service.${service}`, { defaultValue: service });
}
