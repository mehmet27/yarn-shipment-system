import { defineConfig } from 'orval';

const SPEC_DIR = '../../../services';
const MUTATOR = { path: 'src/customInstance.ts', name: 'customInstance' };

function service(name: string, file: string, target: string) {
  return {
    input: `${SPEC_DIR}/${name}/src/main/resources/openapi/${file}`,
    output: {
      mode: 'single' as const,
      target: `src/generated/${target}.ts`,
      client: 'axios-functions' as const,
      override: { mutator: MUTATOR },
    },
  };
}

/** FE-API-01: istemci backend sözleşmelerinden üretilir; `src/generated` elle düzenlenmez. */
export default defineConfig({
  shipment: service('shipment-service', 'shipment-service-api.yaml', 'shipmentService'),
  discrepancy: service('discrepancy-service', 'discrepancy-service-api.yaml', 'discrepancyService'),
  evidence: service('evidence-service', 'evidence-service-api.yaml', 'evidenceService'),
});
