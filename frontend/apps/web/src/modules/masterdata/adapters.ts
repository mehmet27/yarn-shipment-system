import {
  activateDriver,
  activateSite,
  activateSubcontractor,
  activateVehicle,
  createDriver,
  createSite,
  createSubcontractor,
  createVehicle,
  deactivateDriver,
  deactivateSite,
  deactivateSubcontractor,
  deactivateVehicle,
  listDrivers,
  listSites,
  listSubcontractors,
  listVehicles,
  updateDriver,
  updateSite,
  updateSubcontractor,
  updateVehicle,
} from '@iplik/api-client';
import type { ListParams, ManagedKind, RecordPage, RecordValues } from './kinds';

type Adapter = {
  list: (params: ListParams) => Promise<RecordPage>;
  save: (id: string | undefined, values: RecordValues) => Promise<void>;
  setActive: (id: string, isActive: boolean) => Promise<void>;
};

/** Sözleşmedeki ortak sorgu: pasifler yalnızca yönetici için `status=ALL` ile gelir. */
function toQuery({ q, page, size, isShowingInactive }: ListParams) {
  return {
    page,
    size,
    status: isShowingInactive ? ('ALL' as const) : ('ACTIVE' as const),
    ...(q === '' ? {} : { q }),
  };
}

async function discard(request: Promise<unknown>): Promise<void> {
  await request;
}

const subcontractor: Adapter = {
  async list(params) {
    const page = await listSubcontractors(toQuery(params));
    return {
      rows: page.items.map((row) => ({
        id: row.id,
        label: row.title,
        isActive: row.active ?? true,
      })),
      totalPages: page.page.totalPages,
    };
  },
  save: (id, { label }) =>
    discard(
      id === undefined
        ? createSubcontractor({ title: label })
        : updateSubcontractor(id, { title: label }),
    ),
  setActive: (id, isActive) =>
    discard(isActive ? activateSubcontractor(id) : deactivateSubcontractor(id)),
};

/** Şoför satırında fasoncu ünvanı için fasoncu listesi tek sayfada okunur. */
const OWNER_LOOKUP_SIZE = 100;

const driver: Adapter = {
  async list(params) {
    const [page, owners] = await Promise.all([
      listDrivers(toQuery(params)),
      listSubcontractors({ page: 0, size: OWNER_LOOKUP_SIZE, status: 'ALL' }),
    ]);
    const titles = new Map(owners.items.map((owner) => [owner.id, owner.title]));
    return {
      rows: page.items.map((row) => {
        const ownerTitle =
          row.subcontractorId === undefined ? undefined : titles.get(row.subcontractorId);
        return {
          id: row.id,
          label: row.fullName,
          isActive: row.active ?? true,
          ...(row.subcontractorId === undefined ? {} : { subcontractorId: row.subcontractorId }),
          ...(ownerTitle === undefined ? {} : { ownerTitle }),
        };
      }),
      totalPages: page.page.totalPages,
    };
  },
  save(id, { label, subcontractorId }) {
    const body = { fullName: label, ...(subcontractorId === undefined ? {} : { subcontractorId }) };
    return discard(id === undefined ? createDriver(body) : updateDriver(id, body));
  },
  setActive: (id, isActive) => discard(isActive ? activateDriver(id) : deactivateDriver(id)),
};

const vehicle: Adapter = {
  async list(params) {
    const page = await listVehicles(toQuery(params));
    return {
      rows: page.items.map((row) => ({
        id: row.id,
        label: row.plateNo,
        isActive: row.active ?? true,
      })),
      totalPages: page.page.totalPages,
    };
  },
  save: (id, { label }) =>
    discard(
      id === undefined ? createVehicle({ plateNo: label }) : updateVehicle(id, { plateNo: label }),
    ),
  setActive: (id, isActive) => discard(isActive ? activateVehicle(id) : deactivateVehicle(id)),
};

const site: Adapter = {
  async list(params) {
    const page = await listSites(toQuery(params));
    return {
      rows: page.items.map((row) => ({
        id: row.id,
        label: row.name,
        isActive: row.active ?? true,
      })),
      totalPages: page.page.totalPages,
    };
  },
  save: (id, { label }) =>
    discard(id === undefined ? createSite({ name: label }) : updateSite(id, { name: label })),
  setActive: (id, isActive) => discard(isActive ? activateSite(id) : deactivateSite(id)),
};

export const ADAPTERS: Readonly<Record<ManagedKind, Adapter>> = {
  subcontractor,
  driver,
  vehicle,
  site,
};
