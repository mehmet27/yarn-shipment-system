/**
 * Modüller arası senkron çağrı arayüzleri. Çağrılar isteyenin güvenlik bağlamıyla (SecurityContext) yapılır;
 * sahibi olan modül görünürlüğü ve yetkiyi kendisi denetler (SEC-01). Bu paket hiçbir modüle bağımlı olmaz, böylece
 * modüller birbirini doğrudan görmeden (döngüsüz) çağırabilir.
 */
@NullMarked
package com.yarnshipment.moduleapi;

import org.jspecify.annotations.NullMarked;
