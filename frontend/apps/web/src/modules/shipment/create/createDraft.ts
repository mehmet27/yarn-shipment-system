import type { CreateShipmentRequest } from '@iplik/api-client';
import type { MasterOption } from '../hooks/useMasterOptions';

/** Seçilen kayıtlar kimlik ve görünen ad birlikte tutulur; istek ikisini de ister. */
export type CreateDraft = {
  site: MasterOption | undefined;
  subcontractor: MasterOption | undefined;
  driver: MasterOption | undefined;
  vehicle: MasterOption | undefined;
};

export const EMPTY_CREATE_DRAFT: CreateDraft = {
  site: undefined,
  subcontractor: undefined,
  driver: undefined,
  vehicle: undefined,
};

/** i18n anahtarı (`create.errors.*`). */
export type CreateError = 'siteRequired' | 'subcontractorRequired';

export function validateCreate(draft: CreateDraft): CreateError | undefined {
  if (draft.site === undefined) return 'siteRequired';
  return draft.subcontractor === undefined ? 'subcontractorRequired' : undefined;
}

/** Fasoncu değişince önceki fasoncunun şoförü seçili kalmaz. */
export function withSubcontractor(draft: CreateDraft, subcontractor: MasterOption | undefined) {
  return { ...draft, subcontractor, driver: undefined };
}

/** Boş bırakılan şoför ve araç istekte hiç yer almaz. */
export function toCreateRequest(draft: CreateDraft): CreateShipmentRequest | undefined {
  const { site, subcontractor, driver, vehicle } = draft;
  if (site === undefined || subcontractor === undefined) return undefined;
  return {
    siteId: site.value,
    subcontractorId: subcontractor.value,
    subcontractorTitle: subcontractor.label,
    ...(driver === undefined ? {} : { driverId: driver.value, driverName: driver.label }),
    ...(vehicle === undefined ? {} : { vehicleId: vehicle.value, plateNo: vehicle.label }),
  };
}
