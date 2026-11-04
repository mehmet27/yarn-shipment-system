#!/usr/bin/env node
// Depodaki dosya yollarının Windows ve macOS'ta da sorunsuz açılmasını denetler.
// Linux CI büyük/küçük harf duyarlıdır; "Button.tsx" ile "button.tsx" orada iki ayrı dosyadır ama
// Windows'ta biri diğerinin üstüne yazılır ve çalışma klasörü kirli görünür. Aynı şekilde ":" içeren
// ya da "CON" gibi ayrılmış adlı dosyalar Windows'ta hiç oluşturulamaz.
//
// Kullanım (depo kökünde): node tools/ci/check-portable-paths.mjs
import { execFileSync } from "node:child_process";

// Windows'ta çalışma klasörü yolu (ör. C:\Users\ad\Projects\yarn-shipment-system\) ve derleme
// çıktıları (target/, node_modules/) için 260 karakterlik MAX_PATH sınırından pay bırakılır.
const MAX_PATH_LENGTH = 200;
const RESERVED_CHARS = /[<>:"|?*\\\u0000-\u001f]/u;
const RESERVED_NAMES = /^(con|prn|aux|nul|com[0-9¹²³]|lpt[0-9¹²³])(\..*)?$/iu;

const git = (...args) =>
  execFileSync("git", args, { encoding: "utf8", maxBuffer: 64 * 1024 * 1024 });

const entries = git("ls-files", "-s", "-z")
  .split("\0")
  .filter(Boolean)
  .map((line) => {
    const [meta, path] = line.split("\t");
    return { mode: meta.split(" ")[0], path };
  });

const errors = [];

// Yalnızca harf büyüklüğü farklı yollar (dosya ve klasör adları birlikte).
const seen = new Map();
for (const { path } of entries) {
  const segments = path.split("/");
  for (let i = 1; i <= segments.length; i += 1) {
    const prefix = segments.slice(0, i).join("/");
    const key = prefix.toLowerCase();
    const previous = seen.get(key);
    if (previous === undefined) {
      seen.set(key, prefix);
    } else if (previous !== prefix) {
      errors.push(
        `Yalnızca büyük/küçük harfi farklı: "${previous}" ve "${prefix}"`,
      );
      seen.set(key, prefix);
    }
  }
}

// Uzantısız içe aktarmada çakışan modüller: "SettingsForm.tsx" ile "settingsForm.ts" Linux'ta
// ayrı dosyalardır, ama Windows'ta `import './SettingsForm'` önce .ts'i bulur ve yanlış dosyayı açar.
const MODULE_EXTENSION = /\.(?:[cm]?[jt]sx?|d\.ts)$/iu;
const modules = new Map();
for (const { path } of entries) {
  if (!MODULE_EXTENSION.test(path)) {
    continue;
  }
  const key = path.replace(MODULE_EXTENSION, "").toLowerCase();
  const previous = modules.get(key);
  if (previous === undefined) {
    modules.set(key, path);
  } else if (
    previous.replace(MODULE_EXTENSION, "") !==
    path.replace(MODULE_EXTENSION, "")
  ) {
    errors.push(
      `Uzantısız içe aktarmada çakışan modüller (Windows yanlışını açar): "${previous}" ve "${path}"`,
    );
  }
}

for (const { mode, path } of entries) {
  if (mode === "120000") {
    errors.push(
      `Sembolik bağlantı (Windows'ta düz metin dosyası olarak iner): ${path}`,
    );
  }
  if (path.length > MAX_PATH_LENGTH) {
    errors.push(
      `Yol ${String(path.length)} karakter, sınır ${String(MAX_PATH_LENGTH)}: ${path}`,
    );
  }
  for (const segment of path.split("/")) {
    if (RESERVED_CHARS.test(segment)) {
      errors.push(`Windows'ta geçersiz karakter (<>:"|?*\\): ${path}`);
    }
    if (RESERVED_NAMES.test(segment)) {
      errors.push(`Windows'ta ayrılmış ad (CON, NUL, COM1...): ${path}`);
    }
    if (/[. ]$/u.test(segment)) {
      errors.push(
        `Nokta ya da boşlukla biten ad (Windows bunu siler): ${path}`,
      );
    }
  }
}

const unique = [...new Set(errors)];
if (unique.length > 0) {
  for (const error of unique) {
    console.error(`::error::${error}`);
  }
  console.error(
    `\n${String(unique.length)} taşınabilirlik hatası. Dosyayı yeniden adlandırın: git mv eski yeni`,
  );
  process.exit(1);
}
console.log(
  `${String(entries.length)} dosya yolu Windows ve macOS için uygun.`,
);
