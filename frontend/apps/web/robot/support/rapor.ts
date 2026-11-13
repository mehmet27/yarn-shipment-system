import { copyFileSync, existsSync, mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { basename, join } from 'node:path';
import type { FullResult, Reporter, TestCase, TestResult } from '@playwright/test/reporter';

type Kayit = { rol: string; tur: string; ozet: string; ekran?: string };
type Senaryo = {
  ad: string;
  sure: number;
  sorunlar: Kayit[];
  adimlar: { rol: string; ad: string; sonuc: string }[];
  video?: string;
};

const KLASOR = join(import.meta.dirname, '..', 'rapor');
const TUR: Record<string, string> = {
  konsol: 'Konsol hatası',
  ag: 'Sunucu/ağ hatası',
  sayfa: 'Sayfa hatası',
  beklenti: 'Beklenen sonuç gelmedi',
};

const kacis = (metin: string) =>
  metin.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');

/** Senaryo sonuçlarından Türkçe HTML ve Markdown rapor üretir; önceki koşuya göre yeni/çözülen sorunları ayırır. */
export default class RaporReporter implements Reporter {
  private readonly senaryolar: Senaryo[] = [];

  onBegin() {
    rmSync(join(KLASOR, 'dosyalar'), { recursive: true, force: true });
  }

  onTestEnd(test: TestCase, sonuc: TestResult) {
    const ek = sonuc.attachments.find((a) => a.name === 'robot-sonuc');
    const veri =
      ek?.body === undefined
        ? { sorunlar: [], adimlar: [] }
        : (JSON.parse(ek.body.toString()) as Pick<Senaryo, 'sorunlar' | 'adimlar'>);
    const senaryo: Senaryo = { ad: test.title, sure: sonuc.duration, ...veri };
    if (sonuc.status !== 'passed' && senaryo.sorunlar.length === 0 && sonuc.error !== undefined) {
      senaryo.sorunlar.push({
        rol: '-',
        tur: 'sayfa',
        ozet: (sonuc.error.message ?? '').split('\n')[0] ?? 'Senaryo çöktü',
      });
    }
    const video = sonuc.attachments.find((a) => a.name === 'video');
    if (video?.path !== undefined) senaryo.video = video.path;
    this.senaryolar.push(senaryo);
  }

  onEnd(_sonuc: FullResult) {
    mkdirSync(join(KLASOR, 'dosyalar'), { recursive: true });
    for (const s of this.senaryolar) {
      for (const sorun of s.sorunlar) {
        if (sorun.ekran !== undefined && existsSync(sorun.ekran)) {
          const hedef = `dosyalar/${String(this.senaryolar.indexOf(s))}-${basename(sorun.ekran)}`;
          copyFileSync(sorun.ekran, join(KLASOR, hedef));
          sorun.ekran = hedef;
        } else delete sorun.ekran;
      }
      if (s.video !== undefined && existsSync(s.video)) {
        const hedef = `dosyalar/${String(this.senaryolar.indexOf(s))}-video.webm`;
        copyFileSync(s.video, join(KLASOR, hedef));
        s.video = hedef;
      } else delete s.video;
    }
    const anahtar = (s: Senaryo, k: Kayit) => `${s.ad}|${k.rol}|${k.tur}|${k.ozet}`;
    const oncekiYol = join(KLASOR, 'onceki.json');
    const onceki = new Set<string>(
      existsSync(oncekiYol) ? (JSON.parse(readFileSync(oncekiYol, 'utf8')) as string[]) : [],
    );
    const simdi = this.senaryolar.flatMap((s) => s.sorunlar.map((k) => anahtar(s, k)));
    const yeni = new Set(simdi.filter((a) => !onceki.has(a)));
    const cozulen = [...onceki].filter((a) => !simdi.includes(a));
    writeFileSync(oncekiYol, JSON.stringify(simdi));

    const toplam = simdi.length;
    const tekrar = (ad: string) =>
      `pnpm --filter @iplik/web robot -- "--rol=${ad.slice(0, 32).replace(/[()|]/g, '.')}"`;
    const listelenecek = this.senaryolar.flatMap((sc) =>
      sc.sorunlar
        .filter((k) => yeni.size === 0 || yeni.has(anahtar(sc, k)))
        .map((k) => ({ sc, k })),
    );
    const ozetMetni = [
      `Yerel robot raporu (${new Date().toLocaleString('tr-TR')}): ${String(toplam)} sorun, ${String(yeni.size)} yeni, ${String(cozulen.length)} çözülmüş.`,
      '',
      yeni.size > 0
        ? "Aşağıdaki YENİ sorunları incele, nedenini bul ve düzeltme PR'ı aç (önce nedenini bana özetle)."
        : "Aşağıdaki sorunları incele, nedenini bul ve düzeltme PR'ı aç.",
      '',
      ...listelenecek.map(
        ({ sc, k }) =>
          `- [${TUR[k.tur] ?? k.tur}] (${k.rol}) ${k.ozet}\n  Senaryo: ${sc.ad}\n  Yeniden üret: ${tekrar(sc.ad)}`,
      ),
    ].join('\n');
    const adimSayisi = this.senaryolar.reduce((n, s) => n + s.adimlar.length, 0);
    const baslik = new Date().toLocaleString('tr-TR');
    const html = `<!doctype html><html lang="tr"><meta charset="utf-8"><title>Robot raporu</title>
<style>body{font:15px/1.5 system-ui,sans-serif;margin:24px auto;max-width:960px;padding:0 16px}h1{margin-bottom:0}.ozet{padding:12px 16px;border-radius:8px;background:${toplam === 0 ? '#e7f6ec' : '#fdecea'}}
section{border:1px solid #ddd;border-radius:8px;margin:16px 0;padding:8px 16px}.sorun{border-left:4px solid #d33;padding:4px 12px;margin:10px 0;background:#fff7f7}.yeni{background:#d33;color:#fff;border-radius:4px;padding:0 6px;font-size:12px}
.ok{color:#2a7}.bad{color:#d33}img{max-width:100%;border:1px solid #ccc;margin-top:6px}small{color:#666}code{background:#f3f3f3;padding:0 4px}</style>
<h1>Robot raporu</h1><p><label><input type="checkbox" id="yeniler"> Yalnızca YENİ sorunlar</label> <button id="kopya">Sorun metnini kopyala (Claude'a yapıştır)</button></p><small>${kacis(baslik)} · ${String(this.senaryolar.length)} senaryo · ${String(adimSayisi)} adım</small>
<p class="ozet"><b>${String(toplam)}</b> sorun bulundu${yeni.size > 0 ? ` (<b>${String(yeni.size)}</b> yeni)` : ''}${cozulen.length > 0 ? `, önceki koşudan <b>${String(cozulen.length)}</b> sorun çözülmüş` : ''}.</p>
${this.senaryolar
  .map(
    (
      s,
    ) => `<section><h2>${kacis(s.ad)} <small>${String(Math.round(s.sure / 1000))} sn · ${s.sorunlar.length === 0 ? '<span class="ok">sorun yok</span>' : `<span class="bad">${String(s.sorunlar.length)} sorun</span>`}</small></h2>
${s.sorunlar
  .map(
    (k) =>
      `<div class="sorun">${yeni.has(anahtar(s, k)) ? '<span class="yeni">YENİ</span> ' : ''}<b>${kacis(TUR[k.tur] ?? k.tur)}</b> · rol: <code>${kacis(k.rol)}</code><br>${kacis(k.ozet)}${k.ekran === undefined ? '' : `<br><a href="${k.ekran}"><img src="${k.ekran}" alt="ekran görüntüsü"></a>`}</div>`,
  )
  .join('')}
${s.video === undefined ? '' : `<p><a href="${s.video}">Videoyu aç</a></p>`}
<details><summary>${String(s.adimlar.length)} adım</summary><ul>${s.adimlar.map((a) => `<li class="${a.sonuc === 'ok' ? 'ok' : 'bad'}">${a.sonuc === 'ok' ? '✓' : '✗'} <code>${kacis(a.rol)}</code> ${kacis(a.ad)}</li>`).join('')}</ul></details></section>`,
  )
  .join('')}
<textarea id="ozet" hidden>${kacis(ozetMetni)}</textarea>
<script>document.getElementById('yeniler').onchange=(e)=>{document.querySelectorAll('.sorun:not(:has(.yeni))').forEach((d)=>{d.style.display=e.target.checked?'none':''})};document.getElementById('kopya').onclick=async()=>{const t=document.createElement('textarea');t.innerHTML=document.getElementById('ozet').value;await navigator.clipboard.writeText(t.value);document.getElementById('kopya').textContent='Kopyalandı'};</script>
</html>`;
    writeFileSync(join(KLASOR, 'rapor.html'), html);
    const md = [
      `# Robot raporu (${baslik})`,
      '',
      `${String(toplam)} sorun, ${String(yeni.size)} yeni, ${String(cozulen.length)} çözülmüş.`,
      '',
      ...this.senaryolar.flatMap((s) => [
        `## ${s.ad}`,
        ...(s.sorunlar.length === 0
          ? ['Sorun yok.']
          : s.sorunlar.map(
              (k) =>
                `- ${yeni.has(anahtar(s, k)) ? '**[YENİ]** ' : ''}[${TUR[k.tur] ?? k.tur}] (${k.rol}) ${k.ozet}`,
            )),
        '',
      ]),
    ].join('\n');
    writeFileSync(join(KLASOR, 'rapor.md'), md);
    writeFileSync(join(KLASOR, 'sorun-ozeti.md'), ozetMetni);
    writeFileSync(
      join(KLASOR, 'ozet.json'),
      JSON.stringify({ tarih: baslik, sorun: toplam, yeni: yeni.size, cozulen: cozulen.length }),
    );
    console.log(`\nRobot raporu: ${join(KLASOR, 'rapor.html')} (${String(toplam)} sorun)`);
  }
}
