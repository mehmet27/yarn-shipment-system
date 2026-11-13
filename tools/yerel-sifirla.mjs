// Yerel ortamı sıfırlar: Docker verisini (PostgreSQL, Keycloak, MinIO) siler, yalnızca `admin` kullanıcısıyla yeniden kurar
// ve isterseniz backend, gateway ve arayüzü yeniden derleyip başlatır. YALNIZCA YEREL GELİŞTİRME İÇİNDİR; geri alınamaz.
//
//   cd frontend && pnpm yerel:sifirla                 # onay sorar, sıfırlar, yığını yeniden başlatır
//   cd frontend && pnpm yerel:sifirla -- --evet       # onay sormaz
//   --sadece-veri     yalnızca Docker verisini sıfırlar; uygulama, gateway ve arayüze dokunmaz
//   --derleme-atla    backend'i yeniden derlemez (kod değişmediyse süreyi kısaltır)
//   --kok <klasör>    sıfırlanacak proje klasörü (varsayılan: bu betiğin projesi)
import { spawn, spawnSync } from 'node:child_process';
import { closeSync, existsSync, mkdirSync, openSync, readFileSync } from 'node:fs';
import { createInterface } from 'node:readline/promises';
import { join, resolve } from 'node:path';

const arguman = process.argv.slice(2).filter((a) => a !== '--');
const bayrak = (ad) => arguman.includes(ad);
const kokIndex = arguman.indexOf('--kok');
const KOK = resolve(kokIndex >= 0 ? (arguman[kokIndex + 1] ?? '') : join(import.meta.dirname, '..'));
const DEPLOY = join(KOK, 'deploy');
const WIN = process.platform === 'win32';
const KAYIT = join(KOK, '.yerel');

const dur = (mesaj) => {
  console.error(`\nDURDU: ${mesaj}`);
  process.exit(1);
};
// docker, netstat, taskkill gerçek programlardır (kabuk gerekmez; boşluklu yollar bozulmaz). mvnw.cmd/corepack için shell: true verilir.
const calistir = (komut, args, secenek = {}) => spawnSync(komut, args, { encoding: 'utf8', ...secenek });

// ---- Korumalar: yalnızca bu bilgisayardaki yerel Docker ortamı -----------------------------------------------------
const ortamDosyasi = join(DEPLOY, '.env');
if (!existsSync(join(DEPLOY, 'docker-compose.yml')) || !existsSync(join(DEPLOY, 'docker-compose.temiz.yml'))) {
  dur(`${DEPLOY} içinde docker-compose.yml/docker-compose.temiz.yml yok; doğru proje klasöründe misiniz?`);
}
if (!existsSync(ortamDosyasi)) dur('deploy/.env yok (yerel kurulum yapılmamış).');
const ortam = {};
for (const satir of readFileSync(ortamDosyasi, 'utf8').split('\n')) {
  const e = /^\s*([A-Z0-9_]+)\s*=\s*(.*?)\s*$/.exec(satir);
  if (e?.[1] !== undefined) ortam[e[1]] = e[2] ?? '';
}
const riskli = [process.env.SPRING_PROFILES_ACTIVE, process.env.YARNSHIPMENT_ENV, process.env.NODE_ENV, process.env.CI]
  .filter((d) => d !== undefined && /prod|canl|stag|^true$/i.test(d));
if (riskli.length > 0) dur(`Ortam değişkenleri canlı/CI gibi görünüyor (${riskli.join(', ')}). Bu betik yalnızca yerel geliştirme içindir.`);
if (/\.(com|net|org|io)\b/i.test(process.env.DOCKER_HOST ?? '') || (process.env.DOCKER_HOST ?? '').startsWith('ssh://')) {
  dur('DOCKER_HOST uzak bir sunucuyu gösteriyor. Betik yalnızca bu bilgisayardaki Docker ile çalışır.');
}
const baglam = calistir('docker', ['context', 'show']).stdout?.trim() ?? '';
if (baglam !== '' && !/^(default|desktop-linux|desktop-windows|orbstack|colima)$/.test(baglam)) {
  dur(`Docker bağlamı "${baglam}" yerel görünmüyor. Yalnızca varsayılan/Docker Desktop bağlamıyla çalışır.`);
}
const compose = ['compose', '-f', join(DEPLOY, 'docker-compose.yml'), '-f', join(DEPLOY, 'docker-compose.temiz.yml')];

// ---- Onay ----------------------------------------------------------------------------------------------------------
console.log(`Sıfırlanacak proje: ${KOK}
Silinecekler: yerel PostgreSQL, Keycloak ve MinIO verisi (admin dışındaki tüm kullanıcılar, sevkiyatlar, stoklar, kanıtlar).`);
if (!bayrak('--evet')) {
  const okuyucu = createInterface({ input: process.stdin, output: process.stdout });
  const yanit = (await okuyucu.question('Devam etmek için SIL yazın: ')).trim();
  okuyucu.close();
  if (yanit !== 'SIL') dur('Onay verilmedi, hiçbir şey silinmedi.');
}

// ---- Çalışan yerel servisleri durdur (yalnızca bu portları dinleyenler) ---------------------------------------------
const PORTLAR = { 8083: 'uygulama', 8080: 'gateway', 5173: 'arayüz' };
function portuDinleyen(port) {
  if (WIN) {
    const cikti = calistir('netstat', ['-ano', '-p', 'tcp']).stdout ?? '';
    const satir = cikti.split('\n').find((s) => s.includes(`:${String(port)} `) && /LISTENING/i.test(s));
    return satir?.trim().split(/\s+/).pop();
  }
  return calistir('lsof', ['-t', `-iTCP:${String(port)}`, '-sTCP:LISTEN']).stdout?.trim().split('\n')[0] || undefined;
}
function portuKapat(port) {
  const pid = portuDinleyen(port);
  if (pid === undefined || pid === '' || pid === '0') return;
  console.log(`  ${PORTLAR[port]} (${String(port)}) durduruluyor, PID ${pid}`);
  if (WIN) calistir('taskkill', ['/PID', pid, '/T', '/F']);
  else calistir('kill', ['-9', pid]);
}
if (!bayrak('--sadece-veri')) for (const port of Object.keys(PORTLAR)) portuKapat(Number(port));

// ---- Docker verisini sıfırla ---------------------------------------------------------------------------------------
console.log('\nDocker verisi siliniyor ve yalnızca admin kullanıcılı Keycloak ile yeniden kuruluyor...');
const asagi = calistir('docker', [...compose, 'down', '-v'], { stdio: 'inherit' });
if (asagi.status !== 0) dur('docker compose down başarısız.');
const yukari = calistir('docker', [...compose, 'up', '-d'], { stdio: 'inherit' });
if (yukari.status !== 0) dur('docker compose up başarısız.');

async function bekle(ad, adres, kabul, sureSn) {
  const bitis = Date.now() + sureSn * 1000;
  while (Date.now() < bitis) {
    try {
      const yanit = await fetch(adres, { redirect: 'manual', signal: AbortSignal.timeout(3000) });
      if (kabul.includes(yanit.status)) return;
    } catch {
      /* henüz açılmadı */
    }
    await new Promise((r) => setTimeout(r, 3000));
  }
  dur(`${ad} ${String(sureSn)} sn içinde hazır olmadı (${adres}). Günlükler: ${KAYIT}`);
}
await bekle('Keycloak', 'http://localhost:8180/realms/yarnshipment', [200], 240);
console.log('Keycloak hazır.');

if (bayrak('--sadece-veri')) {
  console.log('\nVeri sıfırlandı. Uygulama, gateway ve arayüze dokunulmadı; gerekiyorsa kendiniz yeniden başlatın.');
  process.exit(0);
}

// ---- Backend, gateway ve arayüzü yeniden derle ve başlat -----------------------------------------------------------
mkdirSync(KAYIT, { recursive: true });
const mvnw = WIN ? 'mvnw.cmd' : './mvnw';
if (!bayrak('--derleme-atla')) {
  console.log('\nBackend derleniyor (birkaç dakika sürebilir)...');
  const derle = calistir(mvnw, ['-q', '-DskipTests', '-Djacoco.skip=true', '-pl', 'services/yarnshipment-app,services/gateway', '-am', 'install'], { cwd: KOK, stdio: 'inherit', shell: WIN });
  if (derle.status !== 0) dur('Backend derlenemedi.');
}
function arkaplanda(ad, komut, args, cwd, ek) {
  const gunluk = join(KAYIT, `${ad}.log`);
  const env = { ...process.env, ...ortam, ...ek };
  if (WIN) {
    // `start` servisi ayrı, küçültülmüş bir pencerede başlatır: bu terminalde Ctrl+C ya da pencereyi kapatmak servisleri öldürmez.
    // (Servisin kendi penceresini kapatırsanız o servis durur.)
    const satir = `${komut} ${args.join(' ')} > "${gunluk}" 2>&1`;
    spawn('cmd.exe', ['/d', '/s', '/c', `start "yerel-${ad}" /min cmd.exe /d /c "${satir}"`], {
      cwd,
      env,
      detached: true,
      stdio: 'ignore',
      windowsHide: true,
      windowsVerbatimArguments: true,
    }).unref();
  } else {
    const fd = openSync(gunluk, 'w');
    spawn(komut, args, { cwd, env, detached: true, stdio: ['ignore', fd, fd] }).unref();
    closeSync(fd);
  }
  console.log(`  ${ad} başlatıldı (günlük: .yerel/${ad}.log)`);
}
console.log('\nServisler başlatılıyor...');
arkaplanda('uygulama', mvnw, ['-q', '-pl', 'services/yarnshipment-app', 'spring-boot:run', '-Dspring-boot.run.profiles=local'], KOK, {
  DB_PASSWORD: ortam.LOCAL_DB_PASSWORD ?? '',
  YARNSHIPMENT_KEYCLOAK_ADMIN_CLIENT_SECRET: ortam.KEYCLOAK_ADMIN_CLIENT_SECRET ?? '',
});
arkaplanda('gateway', mvnw, ['-q', '-pl', 'services/gateway', 'spring-boot:run'], KOK, {});
arkaplanda('arayuz', 'corepack', ['pnpm', '--config.engine-strict=false', '--filter', '@iplik/web', 'dev'], join(KOK, 'frontend'), { VITE_USE_MOCK_API: 'false' });

await bekle('Uygulama', 'http://localhost:8083/api/v1/me', [401], 300);
await bekle('Gateway', 'http://localhost:8080/', [200, 302], 300);
await bekle('Arayüz', 'http://localhost:5173/', [200], 120);
console.log(`
Hazır. Yalnızca "admin" kullanıcısı var (şifre: deploy/.env içindeki KEYCLOAK_TEST_USER_PASSWORD).
  Arayüz: http://localhost:5173
Diğer kullanıcıları robot açabilir: cd frontend && pnpm robot -- --rol=Kurulum   (robot/README.md)`);
