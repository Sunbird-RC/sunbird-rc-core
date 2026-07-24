// Configures the local Keycloak (mock National Identity System) for the Age
// Verification demo. Idempotent — safe to re-run.
//
// Creates in realm `sunbird-rc`:
//   - confidential client `national-id-portal` (redirect to the Issuer Portal)
//   - two citizens with National Identity attributes:
//       citizen.over18  (dob 2000-05-20)  password: Passw0rd!
//       citizen.under18 (dob 2010-05-20)  password: Passw0rd!
//
// Env: KC_BASE (http://localhost:8080), KC_REALM (sunbird-rc),
//      KC_ADMIN (admin), KC_ADMIN_PASSWORD (admin123),
//      PORTAL_REDIRECT (http://localhost:4000/callback)
const KC_BASE = process.env.KC_BASE || 'http://localhost:8080';
const KC_REALM = process.env.KC_REALM || 'sunbird-rc';
const KC_ADMIN = process.env.KC_ADMIN || 'admin';
const KC_ADMIN_PASSWORD = process.env.KC_ADMIN_PASSWORD || 'admin123';
const PORTAL_REDIRECT = process.env.PORTAL_REDIRECT || 'http://localhost:4000/callback';
const CLIENT_ID = 'national-id-portal';
const CLIENT_SECRET = 'national-id-portal-secret';

// Legacy (WildFly) Keycloak uses the /auth context path.
const AUTH = `${KC_BASE}/auth`;

async function adminToken() {
  const res = await fetch(`${AUTH}/realms/master/protocol/openid-connect/token`, {
    method: 'POST',
    headers: { 'content-type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'password',
      client_id: 'admin-cli',
      username: KC_ADMIN,
      password: KC_ADMIN_PASSWORD,
    }),
  });
  if (!res.ok) throw new Error(`admin token -> ${res.status}: ${await res.text()}`);
  return (await res.json()).access_token;
}

async function kc(token, method, path, body) {
  const res = await fetch(`${AUTH}/admin/realms/${KC_REALM}${path}`, {
    method,
    headers: { authorization: `Bearer ${token}`, 'content-type': 'application/json' },
    body: body ? JSON.stringify(body) : undefined,
  });
  return res;
}

async function ensureClient(token) {
  const list = await (await kc(token, 'GET', `/clients?clientId=${CLIENT_ID}`)).json();
  const rep = {
    clientId: CLIENT_ID,
    name: 'National Identity Portal',
    enabled: true,
    protocol: 'openid-connect',
    publicClient: false,
    secret: CLIENT_SECRET,
    standardFlowEnabled: true,
    directAccessGrantsEnabled: true,
    redirectUris: [PORTAL_REDIRECT, 'http://localhost:4000/*'],
    webOrigins: ['+'],
    attributes: { 'post.logout.redirect.uris': '+' },
  };
  if (list.length) {
    const id = list[0].id;
    await kc(token, 'PUT', `/clients/${id}`, { ...list[0], ...rep });
    console.log(`  ✓ client ${CLIENT_ID} updated`);
  } else {
    const res = await kc(token, 'POST', '/clients', rep);
    if (!res.ok && res.status !== 409) throw new Error(`create client -> ${res.status}: ${await res.text()}`);
    console.log(`  + client ${CLIENT_ID} created (secret: ${CLIENT_SECRET})`);
  }
}

async function ensureUser(token, u) {
  const list = await (await kc(token, 'GET', `/users?username=${u.username}&exact=true`)).json();
  const rep = {
    username: u.username,
    enabled: true,
    firstName: u.full_name.split(' ')[0],
    lastName: u.full_name.split(' ').slice(1).join(' ') || '.',
    email: `${u.username}@example.gov`,
    emailVerified: true,
    attributes: {
      national_id: [u.national_id],
      full_name: [u.full_name],
      date_of_birth: [u.date_of_birth],
      gender: [u.gender],
    },
    credentials: [{ type: 'password', value: 'Passw0rd!', temporary: false }],
  };
  let id;
  if (list.length) {
    id = list[0].id;
    await kc(token, 'PUT', `/users/${id}`, { ...list[0], ...rep });
    console.log(`  ✓ user ${u.username} updated`);
  } else {
    const res = await kc(token, 'POST', '/users', rep);
    if (!res.ok) throw new Error(`create user ${u.username} -> ${res.status}: ${await res.text()}`);
    // fetch id to (re)set password deterministically
    const created = await (await kc(token, 'GET', `/users?username=${u.username}&exact=true`)).json();
    id = created[0].id;
    console.log(`  + user ${u.username} created`);
  }
  await kc(token, 'PUT', `/users/${id}/reset-password`, { type: 'password', value: 'Passw0rd!', temporary: false });
}

async function main() {
  console.log(`Configuring Keycloak at ${AUTH} (realm ${KC_REALM})`);
  const token = await adminToken();
  await ensureClient(token);
  await ensureUser(token, {
    username: 'citizen.over18',
    national_id: 'NID-1000-0001',
    full_name: 'Aarav Sharma',
    date_of_birth: '2000-05-20',
    gender: 'M',
  });
  await ensureUser(token, {
    username: 'citizen.under18',
    national_id: 'NID-2000-0002',
    full_name: 'Diya Verma',
    date_of_birth: '2010-05-20',
    gender: 'F',
  });
  console.log('Keycloak setup complete. Logins: citizen.over18 / citizen.under18 (password: Passw0rd!)');
}
main().catch((e) => { console.error('KEYCLOAK SETUP FAILED:', e.message); process.exit(1); });
