// Kural kimlikleri: docs/standartlar/react-typescript-kod-standartlari.md
// Sınır değişirse doküman aynı PR'da değişir (FE-GIT-04).
import js from '@eslint/js';
import comments from '@eslint-community/eslint-plugin-eslint-comments/configs';
import vitest from '@vitest/eslint-plugin';
import boundaries from 'eslint-plugin-boundaries';
import checkFile from 'eslint-plugin-check-file';
import i18next from 'eslint-plugin-i18next';
import importX from 'eslint-plugin-import-x';
import jsxA11y from 'eslint-plugin-jsx-a11y';
import react from 'eslint-plugin-react';
import reactHooks from 'eslint-plugin-react-hooks';
import sonarjs from 'eslint-plugin-sonarjs';
import testingLibrary from 'eslint-plugin-testing-library';
import { createTypeScriptImportResolver } from 'eslint-import-resolver-typescript';
import globals from 'globals';
import tseslint from 'typescript-eslint';

const SOURCE = ['**/*.{ts,tsx}'];
const TESTS = ['**/*.test.{ts,tsx}', '**/test/**/*.{ts,tsx}', '**/mocks/**/*.{ts,tsx}'];
const TSCONFIG_PROJECTS = ['apps/*/tsconfig.json', 'packages/*/tsconfig.json'];
const CONFIG_FILES = ['**/*.config.{ts,js}'];

const sizeAndComplexity = {
  'max-lines': ['error', { max: 250, skipBlankLines: true, skipComments: true }], // FE-SIZE-01
  'max-lines-per-function': ['error', { max: 60, skipBlankLines: true, skipComments: true }], // FE-SIZE-02
  'max-statements': ['error', 15], // FE-SIZE-03
  'max-params': ['error', 3], // FE-SIZE-04
  'react/jsx-max-depth': ['error', { max: 6 }], // FE-SIZE-06
  'max-nested-callbacks': ['error', 3], // FE-SIZE-07
  'react/no-multi-comp': ['error', { ignoreStateless: false }], // FE-SIZE-08
  complexity: ['error', 10], // FE-CPLX-01
  'sonarjs/cognitive-complexity': ['error', 12], // FE-CPLX-02
  'max-depth': ['error', 3], // FE-CPLX-03
  'no-nested-ternary': 'error', // FE-CPLX-04
  'sonarjs/no-nested-conditional': 'error', // FE-CPLX-05
  'sonarjs/no-duplicate-string': ['error', { threshold: 3 }], // FE-CPLX-06
  'no-else-return': ['error', { allowElseIf: false }], // FE-CPLX-07
};

const typescriptRules = {
  '@typescript-eslint/ban-ts-comment': [
    'error',
    { 'ts-expect-error': 'allow-with-description', 'ts-ignore': true, 'ts-nocheck': true },
  ], // FE-TS-02
  '@typescript-eslint/no-explicit-any': 'error', // FE-TS-03
  '@typescript-eslint/no-non-null-assertion': 'error', // FE-TS-04
  '@typescript-eslint/no-namespace': 'error', // FE-TS-05
  '@typescript-eslint/consistent-type-assertions': ['error', { assertionStyle: 'never' }], // FE-TS-06 (as const serbest)
  '@typescript-eslint/switch-exhaustiveness-check': [
    'error',
    { considerDefaultExhaustiveForUnions: false, requireDefaultForNonUnion: true },
  ], // FE-TS-07
  '@typescript-eslint/consistent-type-definitions': ['error', 'type'], // FE-TS-08
  '@typescript-eslint/consistent-type-imports': 'error', // FE-TS-09
  '@typescript-eslint/no-floating-promises': 'error', // FE-TS-10
  '@typescript-eslint/no-misused-promises': 'error', // FE-TS-10
  eqeqeq: ['error', 'always'], // FE-TS-12
  '@typescript-eslint/no-unused-vars': ['error', { ignoreRestSiblings: true }], // FE-STRUC-06
  '@typescript-eslint/naming-convention': [
    'error',
    { selector: 'typeLike', format: ['PascalCase'] },
    {
      selector: 'typeLike',
      format: ['PascalCase'],
      custom: { regex: '^[IT][A-Z]', match: false },
    },
    {
      selector: 'variable',
      types: ['boolean'],
      format: ['PascalCase'],
      prefix: ['is', 'has', 'can', 'should'],
    },
    { selector: 'variable', format: ['camelCase', 'PascalCase', 'UPPER_CASE'] },
    { selector: 'function', format: ['camelCase', 'PascalCase'] },
  ], // FE-NAME tablo
};

const structureRules = {
  curly: ['error', 'multi-line'], // FE-STRUC-02
  'import-x/no-default-export': 'error', // FE-STRUC-03
  'no-magic-numbers': 'off',
  '@typescript-eslint/no-magic-numbers': [
    'error',
    {
      ignore: [-1, 0, 1, 2, 100],
      ignoreArrayIndexes: true,
      ignoreEnums: true,
      ignoreTypeIndexes: true,
      ignoreReadonlyClassProperties: true,
    },
  ], // FE-STRUC-04
  'no-console': 'error', // FE-STRUC-07
  'sonarjs/no-commented-code': 'error', // FE-STRUC-08
  'no-warning-comments': ['error', { terms: ['fixme', 'xxx', 'todo(?!\\(#\\d+\\))'] }], // FE-STRUC-08
  'import-x/no-cycle': 'error', // FE-MOD-03
  'import-x/export': 'error',
};

const RESTRICTED_IMPORT_PATHS = {
  axios: { name: 'axios', message: 'FE-MOD-05: axios yalnızca @iplik/api-client içinde.' },
  jwtDecode: { name: 'jwt-decode', message: 'FE-SEC-01: token tarayıcıda okunmaz.' },
};
const RESTRICTED_IMPORT_PATTERNS = {
  mantine: { group: ['@mantine/*'], message: 'FE-MOD-04: Mantine yalnızca @iplik/ui içinde.' },
  deepRelative: { group: ['../../../*'], message: 'FE-MOD-07: @/ takma adını kullanın.' },
};

/** FE-MOD-04/05: kısıtlı kütüphaneyi yalnızca sahibi olan paket import edebilir. */
function restrictImports({ allowAxios = false, allowMantine = false } = {}) {
  const { axios, jwtDecode } = RESTRICTED_IMPORT_PATHS;
  const { mantine, deepRelative } = RESTRICTED_IMPORT_PATTERNS;
  return [
    'error',
    {
      paths: allowAxios ? [jwtDecode] : [axios, jwtDecode],
      patterns: allowMantine ? [deepRelative] : [mantine, deepRelative],
    },
  ];
}

const restrictedImports = {
  'no-restricted-imports': restrictImports(),
  'no-restricted-globals': [
    'error',
    { name: 'fetch', message: 'FE-MOD-05: API çağrısı @iplik/api-client ile yapılır.' },
    { name: 'localStorage', message: 'FE-SEC-05: yalnızca UI tercihleri modülünde.' },
    { name: 'sessionStorage', message: 'FE-SEC-05: yalnızca UI tercihleri modülünde.' },
  ],
  'no-restricted-syntax': [
    'error',
    { selector: 'TSEnumDeclaration', message: 'FE-TS-05: enum yerine birleşim tipi.' },
    { selector: 'ExportAllDeclaration', message: 'FE-MOD-08: export * yasak.' },
    {
      selector:
        'BinaryExpression[operator=/^[!=]==$/] > Literal[value=/^(ADMIN|WAREHOUSE|DRIVER|SUBCONTRACTOR|QUALITY|MANAGER)$/]',
      message: 'FE-SEC-02: rol yerine izin anahtarı (usePermission / <Can>).',
    },
    {
      selector: 'Literal[value=/^#[0-9a-fA-F]{3,8}$|^rgba?\\(/]',
      message: 'FE-UI-02: renkler tema token’larından gelir.',
    },
  ],
};

const reactRules = {
  'react/prefer-stateless-function': 'error', // FE-REACT-01
  'react/no-array-index-key': 'error', // FE-REACT-03
  'react/jsx-key': 'error', // FE-REACT-03
  'react/no-unstable-nested-components': 'error', // FE-REACT-04
  'react/no-danger': 'error', // FE-SEC-06
  'react/jsx-no-target-blank': 'error', // FE-SEC-07
  'react-hooks/rules-of-hooks': 'error', // FE-HOOK-01
  'react-hooks/exhaustive-deps': 'error', // FE-HOOK-01
  'no-eval': 'error', // FE-SEC-06
  'no-implied-eval': 'off',
  '@typescript-eslint/no-implied-eval': 'error', // FE-SEC-06
  'i18next/no-literal-string': ['error', { mode: 'jsx-text-only' }], // FE-UI-05
};

const fileNaming = {
  'check-file/folder-naming-convention': ['error', { '**/src/**/': 'KEBAB_CASE' }],
  'check-file/filename-naming-convention': [
    'error',
    {
      '**/*.tsx': '[a-zA-Z]*([a-zA-Z0-9])',
      '**/*.ts': 'CAMEL_CASE',
    },
    { ignoreMiddleExtensions: true },
  ],
};

/** Uygulama içi modül sınırları (FE-MOD-01, FE-MOD-02). */
const boundaryElements = [
  { type: 'app', pattern: 'apps/*/src/app/**', partialMatch: false },
  { type: 'module', pattern: 'apps/*/src/modules/*', capture: ['appName', 'moduleName'] },
  { type: 'offline', pattern: 'apps/*/src/offline/**', partialMatch: false },
  { type: 'mocks', pattern: 'apps/*/src/mocks/**', partialMatch: false },
];

const moduleEntry = { element: { type: 'module', fileInternalPath: 'index.ts' } };
const elementOfType = (types) => ({ element: { type: types } });

/** FE-MOD-01: modüle yalnızca index.ts'ten girilir. FE-MOD-02: app → modules → offline. */
const boundaryPolicies = [
  { allow: { dependency: { relationship: { to: 'internal' } } } },
  {
    from: elementOfType('app'),
    allow: [{ to: elementOfType(['app', 'offline']) }, { to: moduleEntry }],
  },
  { from: elementOfType('module'), allow: [{ to: elementOfType('offline') }, { to: moduleEntry }] },
  { from: elementOfType('mocks'), allow: [{ to: elementOfType('mocks') }, { to: moduleEntry }] },
];

export function createConfig({ tsconfigRootDir }) {
  return tseslint.config(
    { ignores: ['**/dist/**', '**/coverage/**', '**/generated/**', '**/tools/**', '**/*.js'] },
    js.configs.recommended,
    ...tseslint.configs.strictTypeChecked,
    ...tseslint.configs.stylisticTypeChecked,
    comments.recommended,
    {
      files: SOURCE,
      languageOptions: {
        globals: globals.browser,
        parserOptions: { projectService: true, tsconfigRootDir },
      },
      linterOptions: { reportUnusedDisableDirectives: 'error' },
      settings: {
        react: { version: 'detect' },
        'boundaries/elements': boundaryElements,
        'import/resolver': {
          typescript: { project: TSCONFIG_PROJECTS, noWarnOnMultipleProjects: true },
        },
        'import-x/resolver-next': [
          createTypeScriptImportResolver({
            project: TSCONFIG_PROJECTS,
            noWarnOnMultipleProjects: true,
          }),
        ],
      },
      plugins: {
        react,
        'react-hooks': reactHooks,
        'jsx-a11y': jsxA11y,
        sonarjs,
        'import-x': importX,
        boundaries,
        'check-file': checkFile,
        i18next,
      },
      rules: {
        ...jsxA11y.flatConfigs.recommended.rules, // FE-UI-04
        ...sizeAndComplexity,
        ...typescriptRules,
        ...structureRules,
        ...restrictedImports,
        ...reactRules,
        ...fileNaming,
        '@eslint-community/eslint-comments/require-description': 'error', // FE-GIT-03
        '@eslint-community/eslint-comments/no-unlimited-disable': 'error', // FE-GIT-03
        'boundaries/dependencies': ['error', { default: 'disallow', policies: boundaryPolicies }],
      },
    },
    {
      files: TESTS,
      plugins: { vitest, 'testing-library': testingLibrary },
      rules: {
        ...vitest.configs.recommended.rules,
        ...testingLibrary.configs['flat/react'].rules, // FE-TEST-03
        'vitest/no-focused-tests': 'error', // FE-TEST-06
        'vitest/no-disabled-tests': 'error', // FE-TEST-06
        'max-lines': ['error', { max: 400, skipBlankLines: true, skipComments: true }],
        'max-lines-per-function': 'off',
        'max-statements': 'off',
        'max-nested-callbacks': ['error', 4],
        '@typescript-eslint/no-magic-numbers': 'off',
        '@typescript-eslint/consistent-type-assertions': 'off',
        'sonarjs/no-duplicate-string': 'off',
        'react/no-multi-comp': 'off',
        'i18next/no-literal-string': 'off',
        'boundaries/dependencies': 'off',
      },
    },
    {
      files: ['packages/ui/src/**'],
      rules: { 'no-restricted-imports': restrictImports({ allowMantine: true }) },
    },
    {
      files: ['packages/api-client/src/**'],
      rules: { 'no-restricted-imports': restrictImports({ allowAxios: true }) },
    },
    {
      files: CONFIG_FILES,
      rules: { 'import-x/no-default-export': 'off' },
    },
  );
}
