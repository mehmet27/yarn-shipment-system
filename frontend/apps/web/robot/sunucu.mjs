// Yerel robot yardımcısı: Chrome eklentisi robotu bu sunucu üzerinden başlatır, durumu izler ve raporu açar.
//   pnpm --filter @iplik/web robot:sunucu        (http://127.0.0.1:7777, yalnızca bu bilgisayardan erişilir)
// Güvenlik: yalnızca 127.0.0.1'e bağlanır; durum değiştiren istekler `X-Robot` başlığı ve chrome-extension:// kökeni ister,
// sıradan bir web sayfası bu başlığı CORS ön kontrolünden geçiremez.
import { spawn } from 'node:child_process';
import { createReadStream, existsSync, readFileSync, statSync } from 'node:fs';
import { createServer } from 'node:http';
import { extname, join, normalize } from 'node:path';

const PORT = Number(process.env.ROBOT_SUNUCU_PORT ?? '7777');
const KOK = import.meta.dirname;
const RAPOR = join(KOK, 'rapor');
const TURLER = {
  '.html': 'text/html; charset=utf-8',
  '.md': 'text/markdown; charset=utf-8',
  '.png': 'image/png',
  '.webm': 'video/webm',
  '.json': 'application/json',
};

let cocuk;
let baslangic;
let satirlar = [];

const durum = () => {
  const ozetYolu = join(RAPOR, 'ozet.json');
  const son = existsSync(ozetYolu) ? JSON.parse(readFileSync(ozetYolu, 'utf8')) : undefined;
  return { calisiyor: cocuk !== undefined, baslangic, satirlar: satirlar.slice(-40), son };
};

const kokenIzinli = (istek) => (istek.headers.origin ?? '').startsWith('chrome-extension://');

function cevap(yanit, istek, kod, govde, tur = 'application/json') {
  const baslik = { 'content-type': tur };
  if (kokenIzinli(istek)) {
    baslik['access-control-allow-origin'] = istek.headers.origin;
    baslik['access-control-allow-headers'] = 'content-type, x-robot';
    baslik['access-control-allow-methods'] = 'GET, POST, OPTIONS';
  }
  yanit.writeHead(kod, baslik);
  yanit.end(typeof govde === 'string' ? govde : JSON.stringify(govde));
}

function baslat(secenek) {
  const arguman = [join(KOK, 'run.mjs')];
  if (secenek.izle) arguman.push('--izle');
  if (secenek.video) arguman.push('--video');
  if (secenek.yavas) arguman.push(`--yavas=${String(Number(secenek.yavas))}`);
  if (
    typeof secenek.rol === 'string' &&
    secenek.rol.trim() !== '' &&
    /^[\p{L}\p{N} .:()|_-]+$/u.test(secenek.rol)
  )
    arguman.push(`--rol=${secenek.rol.trim()}`);
  satirlar = [];
  baslangic = new Date().toISOString();
  cocuk = spawn(process.execPath, arguman, { cwd: join(KOK, '..'), env: process.env });
  const topla = (veri) => {
    for (const satir of veri.toString().split(/\r?\n/))
      if (satir.trim() !== '') satirlar.push(satir.slice(0, 300));
    satirlar = satirlar.slice(-200);
  };
  cocuk.stdout.on('data', topla);
  cocuk.stderr.on('data', topla);
  cocuk.on('close', () => {
    cocuk = undefined;
  });
}

const sunucu = createServer((istek, yanit) => {
  const yol = new URL(istek.url ?? '/', 'http://localhost').pathname;
  if (istek.method === 'OPTIONS') return cevap(yanit, istek, kokenIzinli(istek) ? 204 : 403, '');
  if (istek.method === 'GET' && yol === '/durum') return cevap(yanit, istek, 200, durum());
  if (
    istek.method === 'GET' &&
    (yol === '/rapor' || yol.startsWith('/rapor/') || yol === '/sorun-ozeti')
  ) {
    const hedef =
      yol === '/sorun-ozeti'
        ? 'sorun-ozeti.md'
        : yol === '/rapor'
          ? 'rapor.html'
          : normalize(yol.slice('/rapor/'.length));
    const dosya = join(RAPOR, hedef);
    if (hedef.includes('..') || !existsSync(dosya) || !statSync(dosya).isFile())
      return cevap(
        yanit,
        istek,
        404,
        'Rapor yok; önce robotu çalıştırın.',
        'text/plain; charset=utf-8',
      );
    yanit.writeHead(200, {
      'content-type': TURLER[extname(dosya)] ?? 'application/octet-stream',
      ...(kokenIzinli(istek) ? { 'access-control-allow-origin': istek.headers.origin } : {}),
    });
    return createReadStream(dosya).pipe(yanit);
  }
  if (istek.method === 'POST' && (yol === '/baslat' || yol === '/durdur')) {
    if (istek.headers['x-robot'] !== '1' || !kokenIzinli(istek))
      return cevap(yanit, istek, 403, { hata: 'İzin yok' });
    let govde = '';
    istek.on('data', (parca) => (govde += parca));
    istek.on('end', () => {
      if (yol === '/durdur') {
        cocuk?.kill();
        return cevap(yanit, istek, 200, durum());
      }
      if (cocuk !== undefined) return cevap(yanit, istek, 409, { hata: 'Robot zaten çalışıyor' });
      try {
        baslat(govde === '' ? {} : JSON.parse(govde));
      } catch (hata) {
        return cevap(yanit, istek, 400, { hata: String(hata) });
      }
      return cevap(yanit, istek, 202, durum());
    });
    return undefined;
  }
  return cevap(yanit, istek, 404, { hata: 'Bulunamadı' });
});

sunucu.listen(PORT, '127.0.0.1', () => {
  console.log(
    `Robot yardımcısı hazır: http://127.0.0.1:${String(PORT)} (Chrome eklentisi buraya bağlanır; durdurmak için Ctrl+C)`,
  );
});
