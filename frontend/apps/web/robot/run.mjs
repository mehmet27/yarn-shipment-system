// Robot başlatıcı: bayrakları ortam değişkenlerine çevirir (Windows/macOS/Linux'ta aynı çalışır).
//   pnpm robot                  sessiz, rapor üretir
//   pnpm robot -- --izle        gerçek pencerede, yavaşlatarak izlenir (--yavas=500 ms)
//   pnpm robot -- --video       her senaryonun videosunu rapora ekler
//   pnpm robot -- --rol=depo    yalnızca adında "depo" geçen senaryolar
import { spawnSync } from 'node:child_process';
import { join } from 'node:path';

const bayraklar = process.argv.slice(2);
const env = { ...process.env };
const arguman = [];
for (const bayrak of bayraklar) {
  if (bayrak === '--izle') env.ROBOT_IZLE = '1';
  else if (bayrak === '--video') env.ROBOT_VIDEO = '1';
  else if (bayrak.startsWith('--yavas=')) env.ROBOT_YAVAS = bayrak.slice(8);
  else if (bayrak.startsWith('--rol=')) arguman.push('-g', bayrak.slice(6));
  else arguman.push(bayrak);
}
const sonuc = spawnSync('pnpm', ['exec', 'playwright', 'test', '-c', 'robot', ...arguman], {
  stdio: 'inherit',
  env,
  shell: process.platform === 'win32',
  cwd: join(import.meta.dirname, '..'),
});
process.exit(sonuc.status ?? 1);
