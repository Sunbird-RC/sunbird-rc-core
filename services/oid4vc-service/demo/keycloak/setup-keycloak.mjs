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
  // u.attrs is a flat map of attribute -> value; wrap each value in an array
  // (Keycloak stores multi-valued attributes).
  const attributes = Object.fromEntries(Object.entries(u.attrs).map(([k, v]) => [k, [String(v)]]));
  const rep = {
    username: u.username,
    enabled: true,
    firstName: u.full_name.split(' ')[0],
    lastName: u.full_name.split(' ').slice(1).join(' ') || '.',
    email: `${u.username}@example.gov`,
    emailVerified: true,
    attributes,
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

  // Use Case 1 — Age Verification citizens
  await ensureUser(token, { username: 'citizen.over18', full_name: 'Aarav Sharma',
    attrs: { national_id: 'NID-1000-0001', full_name: 'Aarav Sharma', date_of_birth: '2000-05-20', gender: 'M' } });
  await ensureUser(token, { username: 'citizen.under18', full_name: 'Diya Verma',
    attrs: { national_id: 'NID-2000-0002', full_name: 'Diya Verma', date_of_birth: '2010-05-20', gender: 'F' } });

  // Use Case 2 — Agriculture Rural Credit farmers
  await ensureUser(token, { username: 'farmer.male', full_name: 'Ravi Kumar',
    attrs: { farmer_id: 'FRM-1000-0001', full_name: 'Ravi Kumar', gender: 'M',
      land_area_acres: '4.5', ownership_type: 'Owned', land_record_ref: 'LR-KA-77-2201',
      primary_crop: 'Wheat', farm_location: 'Rampur, Karnataka' } });
  await ensureUser(token, { username: 'farmer.female', full_name: 'Lakshmi Devi',
    attrs: { farmer_id: 'FRM-2000-0002', full_name: 'Lakshmi Devi', gender: 'F',
      land_area_acres: '2.0', ownership_type: 'Leased', land_record_ref: 'LR-KA-88-3302',
      primary_crop: 'Paddy', farm_location: 'Shivpur, Karnataka' } });

  // Use Case 3 — Education learners (each maps to one institution)
  await ensureUser(token, { username: 'learner.secondary', full_name: 'Rohan Mehta',
    attrs: { learner_id: 'EDU-1000-0001', full_name: 'Rohan Mehta', institution_name: 'State Secondary Education Board',
      qualification: 'Secondary School Certificate', programme: 'Science', completion_date: '2019-05-15', academic_result: '82%' } });
  await ensureUser(token, { username: 'learner.graduate', full_name: 'Priya Nair',
    attrs: { learner_id: 'EDU-2000-0002', full_name: 'Priya Nair', institution_name: 'State University',
      qualification: 'Bachelor of Science', programme: 'Computer Science', completion_date: '2022-06-30', academic_result: 'First Class' } });
  await ensureUser(token, { username: 'learner.postgraduate', full_name: 'Arjun Rao',
    attrs: { learner_id: 'EDU-3000-0003', full_name: 'Arjun Rao', institution_name: 'National Postgraduate Institute',
      qualification: 'Master of Technology', programme: 'Artificial Intelligence', completion_date: '2024-06-30', academic_result: 'Distinction (CGPA 8.9)' } });

  console.log('Keycloak setup complete. Users (password Passw0rd!):');
  console.log('  UC1: citizen.over18, citizen.under18');
  console.log('  UC2: farmer.male, farmer.female');
  console.log('  UC3: learner.secondary, learner.graduate, learner.postgraduate');
}
main().catch((e) => { console.error('KEYCLOAK SETUP FAILED:', e.message); process.exit(1); });
