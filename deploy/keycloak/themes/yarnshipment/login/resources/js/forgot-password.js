// Giriş formunun altına "Şifremi unuttum" bağlantısı ekler; bağlantı uygulamanın SMS ile sıfırlama sayfasına gider.
(function () {
  "use strict";

  function addLink() {
    var form = document.getElementById("kc-form-login");
    if (!form || document.getElementById("yarnshipment-forgot-password")) {
      return;
    }
    var base = (window.YARNSHIPMENT_FRONTEND_URL || "").replace(/\/+$/, "");
    var wrapper = document.createElement("div");
    wrapper.style.marginTop = "1rem";
    wrapper.style.textAlign = "center";
    var link = document.createElement("a");
    link.id = "yarnshipment-forgot-password";
    link.href = base + "/sifremi-unuttum";
    link.textContent = "Şifremi unuttum";
    wrapper.appendChild(link);
    form.parentNode.insertBefore(wrapper, form.nextSibling);
  }

  // Script form oluşmadan önce yüklenebilir; sayfa hazır değilse DOM hazır olunca çalışır.
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", addLink);
  } else {
    addLink();
  }
})();
