// Yeni ekran için test iskeleti üretir ve ekranı ekran listesine (screens.ts) ekler:
//   pnpm test:e2e:yeni-ekran <dosyaAdi> <ROL> <yol> "<Ekran adı>" "<hazır metni>"
//   pnpm test:e2e:yeni-ekran raporlar ADMIN /panel/raporlar "Raporlar" "Raporlar"
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const ROLES = ['ADMIN', 'WAREHOUSE', 'DRIVER', 'SUBCONTRACTOR', 'QUALITY', 'MANAGER'];
const FIELD_ROLES = ['DRIVER', 'SUBCONTRACTOR'];
const [fileName, role, path, title, ready = title] = process.argv.slice(2);

function fail(message) {
  console.error(message);
  process.exit(1);
}

if (fileName === undefined || title === undefined) {
  fail('Kullanım: pnpm test:e2e:yeni-ekran <dosyaAdi> <ROL> <yol> "<Ekran adı>" ["<hazır metni>"]');
}
if (!/^[a-z][a-zA-Z0-9]*$/.test(fileName)) fail(`Dosya adı camelCase olmalı: ${fileName}`);
if (!ROLES.includes(role)) fail(`Rol şunlardan biri olmalı: ${ROLES.join(', ')}`);
if (!path.startsWith('/')) fail(`Yol / ile başlamalı: ${path}`);

const specUrl = new URL(`../apps/web/e2e/screens/${fileName}.e2e.ts`, import.meta.url);
if (existsSync(specUrl)) fail(`Zaten var: apps/web/e2e/screens/${fileName}.e2e.ts`);

const tag = FIELD_ROLES.includes(role) ? '@saha' : '@panel';
const quote = (text) => `'${text.replaceAll('\\', '\\\\').replaceAll("'", "\\'")}'`;

writeFileSync(
  specUrl,
  `import { expect, test } from '../support/test';

// Ekranın açıldığını ve düzenini ekranlar.e2e.ts denetler; burada ekranın davranışı yazılır.
test.describe(${quote(title)}, { tag: '${tag}' }, () => {
  test('ana akış tamamlanır', { tag: '@smoke' }, async ({ page, openAs }) => {
    await openAs('${role}', ${quote(path)});
    await expect(page.getByText(${quote(ready)}).first()).toBeVisible();
    // Kullanıcının yaptığını adım adım yazın: getByRole / getByLabel ile bulun, tıklayın, doldurun,
    // sonucu expect ile doğrulayın. Örnekler: yeniSevkiyat.e2e.ts, teslimTutanagi.e2e.ts
  });

  test('yetkisi olmayan rol ekranı göremez', async ({ page, openAs }) => {
    // Bu ekranı görmemesi gereken bir rol seçin.
    await openAs('MANAGER', ${quote(path)});
    await expect(page.getByText('Bu sayfayı görme yetkiniz yok.')).toBeVisible();
  });
});
`,
);

const screensPath = fileURLToPath(new URL('../apps/web/e2e/screens.ts', import.meta.url));
const screens = readFileSync(screensPath, 'utf8');
const entry = `  { name: ${quote(title)}, role: '${role}', path: ${quote(path)}, ready: ${quote(ready)} },\n];`;
writeFileSync(screensPath, screens.replace(/\n\];\s*$/, `\n${entry}\n`));

console.log(`Oluşturuldu: apps/web/e2e/screens/${fileName}.e2e.ts
Eklendi: apps/web/e2e/screens.ts (ekran ${tag === '@saha' ? 'saha' : 'panel'} listesine)
Sonra: senaryoyu doldurun, "pnpm format" ve "pnpm test:e2e" çalıştırın.`);
