import { defineConfig } from 'orval';

/** FE-API-01: istemci backend sözleşmesinden üretilir; `src/generated` elle düzenlenmez. */
export default defineConfig({
  shipment: {
    input:
      '../../../services/shipment-service/src/main/resources/openapi/shipment-service-api.yaml',
    output: {
      mode: 'single',
      target: 'src/generated/shipmentService.ts',
      client: 'axios-functions',
      override: {
        mutator: { path: 'src/customInstance.ts', name: 'customInstance' },
      },
    },
  },
  discrepancy: {
    input:
      '../../../services/discrepancy-service/src/main/resources/openapi/discrepancy-service-api.yaml',
    output: {
      mode: 'single',
      target: 'src/generated/discrepancyService.ts',
      client: 'axios-functions',
      override: {
        mutator: { path: 'src/customInstance.ts', name: 'customInstance' },
      },
    },
  },
});
