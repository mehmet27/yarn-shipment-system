// Yerel ortamı yönetir: sıfırlama, servisleri başlatma ve durdurma. YALNIZCA YEREL GELİŞTİRME İÇİNDİR.
//
//   cd frontend
//   pnpm yerel:sifirla              # Docker verisini SİLER (onay sorar), yalnızca admin'li ortam kurar, servisleri başlatır
//   pnpm yerel:sifirla -- --evet    # onay sormaz
//   pnpm yerel:baslat               # veriye dokunmadan servisleri (uygulama, arayüz) yeniden başlatır
//   pnpm yerel:durdur               # servisleri durdurur (Docker'a dokunmaz)
//
//   --sadece-veri     yalnızca Docker verisini sıfırlar; servislere dokunmaz
//   --derleme-atla    backend'i yeniden derlemez (kod değişmediyse süreyi kısaltır)
//   --kok <klasör>    çalışılacak proje klasörü (varsayılan: bu betiğin projesi)
//
// Servisler Windows'ta GİZLİ (pencere açmadan) ve bu terminalden bağımsız başlar: terminal kapansa da çalışmaya devam eder.
// Günlükler ve süreç numaraları .yerel/ altındadır. Bu betik robotu başlatmaz; robot yalnızca siz istediğinizde çalışır.
import { spawn, spawnSync } from "node:child_process";
import {
  closeSync,
  existsSync,
  mkdirSync,
  openSync,
  readFileSync,
  writeFileSync,
} from "node:fs";
import { createInterface } from "node:readline/promises";
import { join, resolve } from "node:path";

const arguman = process.argv.slice(2).filter((a) => a !== "--");
const bayrak = (ad) => arguman.includes(ad);
const kokIndex = arguman.indexOf("--kok");
const KOK = resolve(
  kokIndex >= 0
    ? (arguman[kokIndex + 1] ?? "")
    : join(import.meta.dirname, ".."),
);
const DEPLOY = join(KOK, "deploy");
const WIN = process.platform === "win32";
const KAYIT = join(KOK, ".yerel");
const PID_DOSYASI = join(KAYIT, "surecler.json");
const PORTLAR = { 8083: "uygulama", 5173: "arayüz" };

const dur = (mesaj) => {
  console.error(`\nDURDU: ${mesaj}`);
  process.exit(1);
};
// docker, netstat, taskkill, powershell gerçek programlardır (kabuk gerekmez; boşluklu yollar bozulmaz).
const calistir = (komut, args, secenek = {}) =>
  spawnSync(komut, args, { encoding: "utf8", ...secenek });

// ---- Yerel servisleri durdur (kayıtlı süreçler ve 8083/5173'ü dinleyenler) --------------------------------------
function portuDinleyen(port) {
  if (WIN) {
    const cikti = calistir("netstat", ["-ano", "-p", "tcp"]).stdout ?? "";
    const satir = cikti
      .split("\n")
      .find((s) => s.includes(`:${String(port)} `) && /LISTENING/i.test(s));
    return satir?.trim().split(/\s+/).pop();
  }
  return (
    calistir("lsof", ["-t", `-iTCP:${String(port)}`, "-sTCP:LISTEN"])
      .stdout?.trim()
      .split("\n")[0] || undefined
  );
}
function surecOldur(pid) {
  if (pid === undefined || pid === "" || pid === "0") return;
  if (WIN) calistir("taskkill", ["/PID", String(pid), "/T", "/F"]);
  else calistir("kill", ["-9", String(pid)]);
}
function servisleriDurdur() {
  if (existsSync(PID_DOSYASI)) {
    try {
      for (const pid of Object.values(
        JSON.parse(readFileSync(PID_DOSYASI, "utf8")),
      ))
        surecOldur(String(pid));
    } catch {
      /* bozuk kayıt: portlardan devam edilir */
    }
  }
  for (const [port, ad] of Object.entries(PORTLAR)) {
    const pid = portuDinleyen(Number(port));
    if (pid !== undefined) {
      console.log(`  ${ad} (${port}) durduruluyor, PID ${pid}`);
      surecOldur(pid);
    }
  }
  if (existsSync(PID_DOSYASI)) writeFileSync(PID_DOSYASI, "{}");
}

if (bayrak("--durdur")) {
  console.log("Yerel servisler durduruluyor (Docker'a dokunulmaz)...");
  servisleriDurdur();
  console.log("Tamam.");
  process.exit(0);
}

// ---- Korumalar: yalnızca bu bilgisayardaki yerel ortam ---------------------------------------------------------------
const ortamDosyasi = join(DEPLOY, ".env");
if (!existsSync(join(DEPLOY, "docker-compose.yml"))) {
  dur(`${DEPLOY} içinde docker-compose.yml yok; doğru proje klasöründe misiniz?`);
}
if (!existsSync(ortamDosyasi))
  dur("deploy/.env yok (yerel kurulum yapılmamış).");
const ortam = {};
for (const satir of readFileSync(ortamDosyasi, "utf8").split("\n")) {
  const e = /^\s*([A-Z0-9_]+)\s*=\s*(.*?)\s*$/.exec(satir);
  if (e?.[1] !== undefined) ortam[e[1]] = e[2] ?? "";
}
const riskli = [
  process.env.SPRING_PROFILES_ACTIVE,
  process.env.YARNSHIPMENT_ENV,
  process.env.NODE_ENV,
  process.env.CI,
].filter((d) => d !== undefined && /prod|canl|stag|^true$/i.test(d));
if (riskli.length > 0)
  dur(
    `Ortam değişkenleri canlı/CI gibi görünüyor (${riskli.join(", ")}). Bu betik yalnızca yerel geliştirme içindir.`,
  );
if (
  /\.(com|net|org|io)\b/i.test(process.env.DOCKER_HOST ?? "") ||
  (process.env.DOCKER_HOST ?? "").startsWith("ssh://")
) {
  dur(
    "DOCKER_HOST uzak bir sunucuyu gösteriyor. Betik yalnızca bu bilgisayardaki Docker ile çalışır.",
  );
}
const baglam = calistir("docker", ["context", "show"]).stdout?.trim() ?? "";
if (
  baglam !== "" &&
  !/^(default|desktop-linux|desktop-windows|orbstack|colima)$/.test(baglam)
) {
  dur(
    `Docker bağlamı "${baglam}" yerel görünmüyor. Yalnızca varsayılan/Docker Desktop bağlamıyla çalışır.`,
  );
}
const compose = ["compose", "-f", join(DEPLOY, "docker-compose.yml")];
const sadeceBaslat = bayrak("--sadece-baslat");

async function bekle(ad, adres, kabul, sureSn) {
  const bitis = Date.now() + sureSn * 1000;
  while (Date.now() < bitis) {
    try {
      const yanit = await fetch(adres, {
        redirect: "manual",
        signal: AbortSignal.timeout(3000),
      });
      if (kabul.includes(yanit.status)) return;
    } catch {
      /* henüz açılmadı */
    }
    await new Promise((r) => setTimeout(r, 3000));
  }
  dur(
    `${ad} ${String(sureSn)} sn içinde hazır olmadı (${adres}). Günlükler: ${KAYIT}`,
  );
}

if (!sadeceBaslat) {
  // ---- Onay ve veri sıfırlama --------------------------------------------------------------------------------------
  console.log(`Sıfırlanacak proje: ${KOK}
Silinecekler: yerel PostgreSQL ve MinIO verisi (admin dışındaki tüm kullanıcılar, sevkiyatlar, stoklar, kanıtlar).`);
  if (!bayrak("--evet")) {
    const okuyucu = createInterface({
      input: process.stdin,
      output: process.stdout,
    });
    const yanit = (
      await okuyucu.question("Devam etmek için SIL yazın: ")
    ).trim();
    okuyucu.close();
    if (yanit !== "SIL") dur("Onay verilmedi, hiçbir şey silinmedi.");
  }
  if (!bayrak("--sadece-veri")) servisleriDurdur();
  console.log(
    "\nDocker verisi siliniyor ve yeniden kuruluyor...",
  );
  if (
    calistir("docker", [...compose, "down", "-v"], { stdio: "inherit" })
      .status !== 0
  )
    dur("docker compose down başarısız.");
  if (
    calistir("docker", [...compose, "up", "-d"], { stdio: "inherit" })
      .status !== 0
  )
    dur("docker compose up başarısız.");
  if (bayrak("--sadece-veri")) {
    console.log(
      "\nVeri sıfırlandı. Servislere dokunulmadı; gerekiyorsa `pnpm yerel:baslat` ile başlatın.",
    );
    process.exit(0);
  }
} else {
  servisleriDurdur();
}

// ---- Backend ve arayüzü (yeniden) derle ve başlat ----------------------------------------------------------
mkdirSync(KAYIT, { recursive: true });
const mvnw = WIN ? ".\\mvnw.cmd" : "./mvnw";
if (!bayrak("--derleme-atla")) {
  console.log("\nBackend derleniyor (birkaç dakika sürebilir)...");
  const derle = calistir(
    mvnw,
    [
      "-q",
      "-DskipTests",
      "-Djacoco.skip=true",
      "-pl",
      "services/yarnshipment-app",
      "-am",
      "install",
    ],
    {
      cwd: KOK,
      stdio: "inherit",
      shell: WIN,
    },
  );
  if (derle.status !== 0) dur("Backend derlenemedi.");
}

const surecler = {};
function arkaplanda(ad, komut, args, cwd, ek) {
  const gunluk = join(KAYIT, `${ad}.log`);
  const env = { ...process.env, ...ortam, ...ek };
  if (WIN) {
    // Start-Process -WindowStyle Hidden: pencere açmaz, bu terminalden bağımsızdır (terminal kapansa ya da Ctrl+C yapılsa da ölmez).
    const tek = (d) => `'${d.replaceAll("'", "''")}'`;
    const komutSatiri = `${komut} ${args.join(" ")}`;
    const pidDosyasi = join(KAYIT, `${ad}.pid`);
    const betik = `$p = Start-Process -FilePath cmd.exe -ArgumentList '/d','/c',${tek(komutSatiri)} -WorkingDirectory ${tek(cwd)} -WindowStyle Hidden -RedirectStandardOutput ${tek(gunluk)} -RedirectStandardError ${tek(`${gunluk}.err`)} -PassThru; Set-Content -Path ${tek(pidDosyasi)} -Value $p.Id`;
    // stdio 'ignore': uzun yaşayan çocuk süreç boruyu devralıp bu betiği kilitlemesin.
    calistir(
      "powershell.exe",
      ["-NoProfile", "-NonInteractive", "-Command", betik],
      { env, stdio: "ignore" },
    );
    surecler[ad] = existsSync(pidDosyasi)
      ? readFileSync(pidDosyasi, "utf8").trim()
      : "";
  } else {
    const fd = openSync(gunluk, "w");
    const cocuk = spawn(komut, args, {
      cwd,
      env,
      detached: true,
      stdio: ["ignore", fd, fd],
    });
    surecler[ad] = String(cocuk.pid);
    cocuk.unref();
    closeSync(fd);
  }
  console.log(`  ${ad} başlatıldı (günlük: .yerel/${ad}.log)`);
}
console.log("\nServisler başlatılıyor (pencere açılmaz)...");
arkaplanda(
  "uygulama",
  mvnw,
  [
    "-q",
    "-pl",
    "services/yarnshipment-app",
    "spring-boot:run",
    "-Dspring-boot.run.profiles=local",
  ],
  KOK,
  {
    DB_PASSWORD: ortam.LOCAL_DB_PASSWORD ?? "",
    // Yalnızca admin'li temiz ortam: örnek kullanıcılar açılmaz; admin şifresi LOCAL_TEST_USER_PASSWORD'dür.
    LOCAL_SAMPLE_USERS: "false",
  },
);
arkaplanda(
  "arayuz",
  "corepack",
  ["pnpm", "--config.engine-strict=false", "--filter", "@iplik/web", "dev"],
  join(KOK, "frontend"),
  {
    VITE_USE_MOCK_API: "false",
  },
);
writeFileSync(PID_DOSYASI, JSON.stringify(surecler));

await bekle("Uygulama", "http://localhost:8083/api/v1/me", [401], 300);
await bekle("Arayüz", "http://localhost:5173/", [200], 120);
console.log(`
Hazır. ${sadeceBaslat ? "" : 'Yalnızca "admin" kullanıcısı var (şifre: deploy/.env içindeki LOCAL_TEST_USER_PASSWORD).\n  '}Arayüz: http://localhost:5173
Servisleri durdurmak: cd frontend && pnpm yerel:durdur
Diğer kullanıcıları robot açabilir (siz isterseniz): cd frontend && pnpm robot -- --rol=Kurulum`);
