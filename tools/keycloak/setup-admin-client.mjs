// Mevcut yarnshipment realm'ine, shipment-service'in giriş hesabı açmak için kullandığı `yarnshipment-admin`
// servis hesabı istemcisini ekler. Realm dosyası yalnızca realm yokken içe aktarıldığı için, daha önce kurulmuş
// yerel Keycloak'larda bu betik çalıştırılır; yeni kurulumlarda realm dosyası bunu zaten içerir. Tekrarı zararsızdır.
// Kullanım (Node 22):
//   KEYCLOAK_ADMIN_PASSWORD=<deploy/.env değeri> KEYCLOAK_ADMIN_CLIENT_SECRET=<seçtiğiniz sır> \
//     node tools/keycloak/setup-admin-client.mjs
const baseUrl = process.env.KEYCLOAK_URL ?? 'http://localhost:8180';
const realm = 'yarnshipment';
const clientId = 'yarnshipment-admin';
const adminPassword = process.env.KEYCLOAK_ADMIN_PASSWORD;
const clientSecret = process.env.KEYCLOAK_ADMIN_CLIENT_SECRET ?? 'local-admin-client-secret';
const grantedRoles = ['manage-users', 'view-users', 'view-realm'];
if (!adminPassword) {
  console.error('KEYCLOAK_ADMIN_PASSWORD tanımlı değil');
  process.exit(2);
}

async function call(path, token, init = {}) {
  const response = await fetch(`${baseUrl}${path}`, {
    ...init,
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json', ...init.headers },
  });
  if (!response.ok) {
    throw new Error(`${init.method ?? 'GET'} ${path}: HTTP ${response.status} ${await response.text()}`);
  }
  return response.status === 204 ? null : response;
}

const tokenResponse = await fetch(`${baseUrl}/realms/master/protocol/openid-connect/token`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  body: new URLSearchParams({
    grant_type: 'password',
    client_id: 'admin-cli',
    username: 'admin',
    password: adminPassword,
  }),
});
if (!tokenResponse.ok) {
  throw new Error(`Yönetici token'ı alınamadı (HTTP ${tokenResponse.status})`);
}
const { access_token: token } = await tokenResponse.json();
const admin = `/admin/realms/${realm}`;

const existing = await (await call(`${admin}/clients?clientId=${clientId}`, token)).json();
if (existing.length > 0) {
  console.log(`${clientId} zaten var; yetkileri kontrol ediliyor`);
}
let internalId = existing[0]?.id;
if (!internalId) {
  await call(`${admin}/clients`, token, {
    method: 'POST',
    body: JSON.stringify({
      clientId,
      name: 'Hesap yönetimi (servis hesabı)',
      enabled: true,
      publicClient: false,
      secret: clientSecret,
      standardFlowEnabled: false,
      directAccessGrantsEnabled: false,
      serviceAccountsEnabled: true,
    }),
  });
  internalId = (await (await call(`${admin}/clients?clientId=${clientId}`, token)).json())[0].id;
  console.log(`${clientId} oluşturuldu`);
}

const serviceUser = await (await call(`${admin}/clients/${internalId}/service-account-user`, token)).json();
const management = (await (await call(`${admin}/clients?clientId=realm-management`, token)).json())[0].id;
const roles = [];
for (const name of grantedRoles) {
  roles.push(await (await call(`${admin}/clients/${management}/roles/${name}`, token)).json());
}
await call(`${admin}/users/${serviceUser.id}/role-mappings/clients/${management}`, token, {
  method: 'POST',
  body: JSON.stringify(roles),
});
console.log(`Yetkiler verildi: ${grantedRoles.join(', ')}`);
