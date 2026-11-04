import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { afterEach, describe, expect, it } from 'vitest';

const SCRIPT = readFileSync(
  resolve(
    __dirname,
    '../../../../../deploy/keycloak/themes/yarnshipment/login/resources/js/forgot-password.js',
  ),
  'utf8',
);
const LINK_ID = 'yarnshipment-forgot-password';

function runScript() {
  // eslint-disable-next-line @typescript-eslint/no-implied-eval -- Tema betiği tarayıcıda düz script olarak çalışır; testte de öyle çalıştırılır.
  const run = new Function(SCRIPT) as () => void;
  run();
}

describe('Keycloak giriş teması: Şifremi unuttum bağlantısı', () => {
  afterEach(() => {
    document.body.innerHTML = '';
  });

  it('form sayfada varsa bağlantı hemen eklenir ve iki kez eklenmez', () => {
    document.body.innerHTML = '<form id="kc-form-login"></form>';
    runScript();
    runScript();
    expect(document.querySelectorAll(`#${LINK_ID}`)).toHaveLength(1);
    expect(document.getElementById(LINK_ID)?.getAttribute('href')).toBe('/sifremi-unuttum');
  });

  it('sayfa hâlâ yükleniyorsa form oluşunca (DOMContentLoaded) bağlantı eklenir', () => {
    Object.defineProperty(document, 'readyState', { value: 'loading', configurable: true });
    try {
      runScript(); // Form henüz yok.
      document.body.innerHTML = '<form id="kc-form-login"></form>';
      expect(document.getElementById(LINK_ID)).toBeNull();
      Object.defineProperty(document, 'readyState', { value: 'complete', configurable: true });
      document.dispatchEvent(new Event('DOMContentLoaded'));
      expect(document.getElementById(LINK_ID)).not.toBeNull();
    } finally {
      Reflect.deleteProperty(document, 'readyState');
    }
  });
});
