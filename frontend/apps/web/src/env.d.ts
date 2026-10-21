/// <reference types="vite/client" />

// eslint-disable-next-line @typescript-eslint/consistent-type-definitions -- Vite tipini genişletmek için declaration merging gerekir (FE-TS-08)
interface ImportMetaEnv {
  readonly VITE_USE_MOCK_API?: string;
}
