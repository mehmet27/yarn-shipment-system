const ADRES = 'http://127.0.0.1:7777';
const $ = (kimlik) => document.getElementById(kimlik);

async function istek(yol, secenek = {}) {
  const yanit = await fetch(ADRES + yol, {
    ...secenek,
    headers: { 'X-Robot': '1', 'Content-Type': 'application/json' },
  });
  return yanit;
}

async function yenile() {
  try {
    const durum = await (await istek('/durum')).json();
    const kutu = $('durum');
    $('baslat').disabled = durum.calisiyor;
    $('durdur').disabled = !durum.calisiyor;
    if (durum.calisiyor) {
      kutu.className = 'calisiyor';
      kutu.textContent = `Çalışıyor (başladı ${new Date(durum.baslangic).toLocaleTimeString('tr-TR')})`;
    } else if (durum.son) {
      kutu.className = durum.son.sorun > 0 ? 'sorun' : 'temiz';
      kutu.textContent = `Son koşu ${durum.son.tarih}: ${durum.son.sorun} sorun, ${durum.son.yeni} yeni, ${durum.son.cozulen} çözülmüş`;
    } else {
      kutu.className = '';
      kutu.textContent = 'Henüz rapor yok. Robotu başlatın.';
    }
    const gunluk = $('gunluk');
    gunluk.hidden = durum.satirlar.length === 0;
    gunluk.textContent = durum.satirlar.slice(-12).join('\n');
  } catch {
    $('durum').className = 'sorun';
    $('durum').textContent =
      'Yardımcı sunucuya bağlanılamadı. Terminalde: pnpm --filter @iplik/web robot:sunucu';
    $('baslat').disabled = true;
  }
}

$('baslat').onclick = async () => {
  const yanit = await istek('/baslat', {
    method: 'POST',
    body: JSON.stringify({
      izle: $('izle').checked,
      video: $('video').checked,
      rol: $('rol').value,
    }),
  });
  $('not').textContent = yanit.status === 202 ? 'Başlatıldı.' : `Başlatılamadı (${yanit.status}).`;
  yenile();
};
$('durdur').onclick = async () => {
  await istek('/durdur', { method: 'POST', body: '{}' });
  yenile();
};
$('rapor').onclick = () => chrome.tabs.create({ url: `${ADRES}/rapor` });
$('kopyala').onclick = async () => {
  const yanit = await istek('/sorun-ozeti');
  const metin = await yanit.text();
  await navigator.clipboard.writeText(metin);
  $('not').textContent = yanit.ok
    ? "Sorun metni panoya kopyalandı; Claude'a yapıştırın."
    : 'Önce robotu çalıştırıp rapor üretin.';
};

yenile();
setInterval(yenile, 2000);
