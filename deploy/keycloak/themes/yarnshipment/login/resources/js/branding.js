// Giriş ve şifre değiştirme ekranlarına uygulamayla aynı görünümü verir: koyu marka gradyanı, beyaz kart, marka renkli
// düğme. Renk tek değerden (YARNSHIPMENT_BRAND_COLOR) türetilir; açık ve koyu tonlar tarayıcıda karıştırılır.
(function () {
  "use strict";

  var color = /^#[0-9a-fA-F]{6}$/.test(window.YARNSHIPMENT_BRAND_COLOR || "")
    ? window.YARNSHIPMENT_BRAND_COLOR
    : "#17527a";

  var css =
    ":root{--ys-brand:" + color + ";" +
    "--ys-brand-dark:color-mix(in srgb," + color + " 55%,black);" +
    "--ys-brand-light:color-mix(in srgb," + color + " 12%,white);}" +
    "html,body,.pf-v5-c-login{background:linear-gradient(135deg,var(--ys-brand),var(--ys-brand-dark)) fixed !important;}" +
    ".pf-v5-c-login__container{padding-top:2rem;}" +
    ".pf-v5-c-login__main{border-radius:16px;overflow:hidden;box-shadow:0 20px 48px rgba(0,0,0,.35);}" +
    ".pf-v5-c-login__main-header{border-top:4px solid var(--ys-brand);}" +
    ".pf-v5-c-login__header,#kc-header-wrapper{color:#fff !important;text-align:center;font-weight:700;}" +
    "#kc-header-wrapper{font-size:1.75rem;letter-spacing:.01em;text-transform:none;}" +
    ".pf-v5-c-login__main-header .pf-v5-c-title{color:var(--ys-brand-dark);}" +
    // Koyu kartta (koyu tema) koyu marka rengi okunmaz; başlık ve bağlantılar açık tona çekilir.
    "@media (prefers-color-scheme:dark){:root{--ys-accent:color-mix(in srgb," + color + " 35%,white);}}" +
    "@media (prefers-color-scheme:light){:root{--ys-accent:var(--ys-brand-dark);}}" +
    "html.pf-v5-theme-dark{--ys-accent:color-mix(in srgb," + color + " 35%,white);}" +
    ".pf-v5-c-login__main-header .pf-v5-c-title,.pf-v5-c-login__main-header h1{color:var(--ys-accent) !important;}" +
    "a,.pf-v5-c-button.pf-m-link,#kc-form-buttons ~ * a{color:var(--ys-accent);}" +
    // Dar ekranda kart kenarlara yapışmasın.
    "@media (max-width:576px){.pf-v5-c-login__container{padding:1rem;}.pf-v5-c-login__main{margin:0 auto;max-width:100%;}}" +
    ":root{--pf-v5-global--primary-color--100:var(--ys-brand);--pf-v5-global--primary-color--200:var(--ys-brand-dark);" +
    "--pf-v5-global--link--Color:var(--ys-brand);--pf-v5-global--link--Color--hover:var(--ys-brand-dark);}" +
    ".pf-v5-c-button.pf-m-primary{background:var(--ys-brand);min-height:44px;border-radius:10px;font-weight:600;}" +
    ".pf-v5-c-button.pf-m-primary:hover,.pf-v5-c-button.pf-m-primary:focus{background:var(--ys-brand-dark);}" +
    ".pf-v5-c-form-control{min-height:44px;}" +
    ".pf-v5-c-form-control:focus-within{--pf-v5-c-form-control--after--BorderBottomColor:var(--ys-brand);}" +
    ".pf-v5-c-login__footer,#kc-info,#kc-info-wrapper{color:var(--ys-brand-light);}" +
    "#yarnshipment-forgot-password{color:var(--ys-accent);font-weight:600;text-decoration:underline;}";

  function renameHeader() {
    var header = document.getElementById("kc-header-wrapper");
    if (header && window.YARNSHIPMENT_BRAND_NAME) {
      header.textContent = window.YARNSHIPMENT_BRAND_NAME;
    }
  }

  // Stil hemen eklenir (açılışta beyaz parlama olmasın); başlık metni DOM hazır olunca değişir.
  var style = document.createElement("style");
  style.id = "yarnshipment-branding";
  style.textContent = css;
  (document.head || document.documentElement).appendChild(style);
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", renameHeader);
  } else {
    renameHeader();
  }
})();
