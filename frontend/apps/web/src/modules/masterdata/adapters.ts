import {
  activateDriver,
  activateSubcontractor,
  activateVehicle,
  createDriver,
  createSubcontractor,
  createVehicle,
  deactivateDriver,
  deactivateSubcontractor,
  deactivateVehicle,
  listDrivers,
  listSubcontractors,
  listVehicles,
  updateDriver,
  updateSubcontractor,
  updateVehicle,
} from '@iplik/api-client';
import type { ManagedKind } from './kinds';
import { discard, product, qualityGrade, toQuery, yarnSupplier } from './catalogAdapters';
import type { Adapter } from './catalogAdapters';

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

const driver: Adapter = {
  async list(params) {
    const page = await listDrivers(toQuery(params));
    return {
      rows: page.items.map((row) => ({
        id: row.id,
        label: row.fullName,
        isActive: row.active ?? true,
        ...(row.subcontractorId === undefined ? {} : { subcontractorId: row.subcontractorId }),
        ...(row.subcontractorTitle === undefined ? {} : { ownerTitle: row.subcontractorTitle }),
      })),
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

export const ADAPTERS: Readonly<Record<ManagedKind, Adapter>> = {
  subcontractor,
  driver,
  vehicle,
  product,
  qualityGrade,
  yarnSupplier,
};
