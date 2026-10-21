// Yerel Keycloak'taki yarnshipment realm'ini doğrular: her test kullanıcısı için token alır ve servislerin
// beklediği claim'lerin bulunduğunu denetler. Kullanım (Node 22):
//   KEYCLOAK_TEST_USER_PASSWORD=<deploy/.env değeri> node tools/keycloak/verify-realm.mjs
const baseUrl = process.env.KEYCLOAK_URL ?? 'http://localhost:8180';
const password = process.env.KEYCLOAK_TEST_USER_PASSWORD;
if (!password) {
  console.error('KEYCLOAK_TEST_USER_PASSWORD tanımlı değil');
  process.exit(2);
}

const users = [
  { username: 'admin', role: 'admin' },
  { username: 'depo', role: 'warehouse' },
  { username: 'kalite', role: 'quality' },
  { username: 'mudur', role: 'manager' },
  { username: 'sofor', role: 'driver', scopeClaim: 'driver_id' },
  { username: 'fasoncu', role: 'subcontractor', scopeClaim: 'subcontractor_id' },
];

async function tokenFor(username) {
  const response = await fetch(`${baseUrl}/realms/yarnshipment/protocol/openid-connect/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'password',
      client_id: 'yarnshipment-dev-tools',
      username,
      password,
    }),
  });
  if (!response.ok) {
    throw new Error(`${username}: token alınamadı (HTTP ${response.status}) ${await response.text()}`);
  }
  const { access_token: accessToken } = await response.json();
  return JSON.parse(Buffer.from(accessToken.split('.')[1], 'base64url').toString('utf8'));
}

function check(condition, message, problems) {
  if (!condition) {
    problems.push(message);
  }
}

let failed = false;
for (const user of users) {
  const problems = [];
  try {
    const claims = await tokenFor(user.username);
    check(claims.roles?.includes(user.role), `roles claim'inde ${user.role} yok: ${JSON.stringify(claims.roles)}`, problems);
    check([claims.aud].flat().includes('yarnshipment-api'), `aud yarnshipment-api içermiyor: ${JSON.stringify(claims.aud)}`, problems);
    check(Array.isArray(claims.site_ids) && claims.site_ids.length > 0, 'site_ids boş', problems);
    check(Boolean(claims.name), 'name claim\'i yok', problems);
    if (user.scopeClaim) {
      check(Boolean(claims[user.scopeClaim]), `${user.scopeClaim} claim'i yok`, problems);
    }
  } catch (error) {
    problems.push(error.message);
  }
  console.log(`${problems.length === 0 ? 'OK  ' : 'HATA'} ${user.username}${problems.map((p) => `\n       ${p}`).join('')}`);
  failed ||= problems.length > 0;
}
process.exit(failed ? 1 : 0);
