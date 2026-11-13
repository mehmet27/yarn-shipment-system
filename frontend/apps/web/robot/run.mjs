// Robot başlatıcı: bayrakları ortam değişkenlerine çevirir (Windows/macOS/Linux'ta aynı çalışır).
//   pnpm robot                  sessiz, rapor üretir
//   pnpm robot -- --izle        gerçek pencerede, yavaşlatarak izlenir (--yavas=500 ms)
//   pnpm robot -- --video       her senaryonun videosunu rapora ekler
//   pnpm robot -- --rol=depo    yalnızca adında "depo" geçen senaryolar
import { spawnSync } from 'node:child_process';
import { createRequire } from 'node:module';
import { existsSync, readFileSync } from 'node:fs';
import { join } from 'node:path';

const bayraklar = process.argv.slice(2);
const env = { ...process.env };

// robot/.env.local (git'e girmez): her satır ANAHTAR=değer, ör. ROBOT_SIFRE=Robot!2026x
const yerel = join(import.meta.dirname, '.env.local');
if (existsSync(yerel)) {
  for (const satir of readFileSync(yerel, 'utf8').split('\n')) {
    const eslesme = /^\s*([A-Z_]+)\s*=\s*(.*?)\s*$/.exec(satir);
    if (eslesme?.[1] !== undefined && env[eslesme[1]] === undefined)
      env[eslesme[1]] = eslesme[2] ?? '';
  }
}
if (env.ROBOT_SIFRE === undefined || env.ROBOT_SIFRE === '') {
  console.error(`ROBOT_SIFRE tanımlı değil; robot kullanıcıların şifresini bilmeden giriş yapamaz.
  PowerShell : $env:ROBOT_SIFRE = 'Robot!2026x'
  Git Bash   : export ROBOT_SIFRE='Robot!2026x'
  Kalıcı     : robot/.env.local dosyasına  ROBOT_SIFRE=Robot!2026x  yazın`);
  process.exit(2);
}

const arguman = [];
for (const bayrak of bayraklar) {
  if (bayrak === '--') continue;
  if (bayrak === '--izle') env.ROBOT_IZLE = '1';
  else if (bayrak === '--video') env.ROBOT_VIDEO = '1';
  else if (bayrak.startsWith('--yavas=')) env.ROBOT_YAVAS = bayrak.slice(8);
  else if (bayrak.startsWith('--rol=')) arguman.push('-g', bayrak.slice(6));
  else arguman.push(bayrak);
}
// Playwright'ı doğrudan node ile çalıştırırız: kabuk üzerinden geçince Windows'ta `|` ve boşluk bozulur.
const playwright = createRequire(import.meta.url).resolve('@playwright/test/cli');
const sonuc = spawnSync(process.execPath, [playwright, 'test', '-c', 'robot', ...arguman], {
  stdio: 'inherit',
  env,
  cwd: join(import.meta.dirname, '..'),
});
process.exit(sonuc.status ?? 1);
