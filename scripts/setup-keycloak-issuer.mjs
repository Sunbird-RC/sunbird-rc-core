// Configures Keycloak as the SINGLE identity provider for the issuer portal,
// the registry and wallet self-service issuance. Idempotent — safe to re-run.
//
// Creates in realm `sunbird-rc`:
//   - realm roles `issuer-staff` (portal CRUD + offer creation) and `citizen`
//     (self-service issuance only)
//   - confidential client `issuer-portal`   — staff login, standard flow
//   - public client      `sunbird-wallet`   — citizen login from the wallet,
//                                             PKCE S256 REQUIRED
//   - a `farmerId` user-attribute mapper on a dedicated client scope, so the
//     claim lands in BOTH the ID token and the access token. oid4vc-service
//     reads it straight off the presented token, avoiding an admin API call on
//     every credential request.
//   - sample staff + citizen users
//
// Why the claim rather than a lookup by username or email: those change, and an
// unverified email is not an identity. `farmerId` is written by the portal when
// it links a record to a login, and Keycloak's `sub` is mirrored onto the
// registry record as the reverse index.
//
// Usage (from the repository root):
//   KC_BASE=https://98.70.36.106.sslip.io KC_ADMIN=… KC_ADMIN_PASSWORD=… \
//   PORTAL_PUBLIC_URL=https://98.70.36.106.sslip.io \
//   node scripts/setup-keycloak-issuer.mjs
//
// Pass WALLET_REDIRECT_URIS as a comma-separated list once the wallet's real
// deep-link scheme is known; the placeholder below will not work with a real
// wallet build.

const KC_BASE = process.env.KC_BASE || 'http://localhost:8080';
const KC_REALM = process.env.KC_REALM || 'sunbird-rc';
const KC_ADMIN = process.env.KC_ADMIN || 'admin';
const KC_ADMIN_PASSWORD = process.env.KC_ADMIN_PASSWORD || 'admin123';

// Where the portal is reachable from a BROWSER. The redirect_uri must match
// this exactly, so it is the public gateway URL and never the internal DNS name.
const PORTAL_PUBLIC_URL = process.env.PORTAL_PUBLIC_URL || 'http://localhost:4100';
const PORTAL_BASE_PATH = process.env.PORTAL_BASE_PATH || '/issuer-portal';

const PORTAL_CLIENT_ID = process.env.ISSUER_PORTAL_CLIENT_ID || 'issuer-portal';
const PORTAL_CLIENT_SECRET = process.env.ISSUER_PORTAL_CLIENT_SECRET || 'issuer-portal-secret';
const WALLET_CLIENT_ID = process.env.WALLET_CLIENT_ID || 'sunbird-wallet';

// Real wallets present their OWN client id, which we do not choose. Paradym uses
// this one, and Keycloak refuses an unknown client with a bare "Client not found"
// page before any login form — so the client has to exist even though nothing
// here owns it. It used to be created by hand on the deployment; a realm rebuilt
// from this script alone therefore came up unable to complete a single wallet
// flow. Add more ids here as other wallets are tested.
const EXTERNAL_WALLET_CLIENT_IDS = (process.env.EXTERNAL_WALLET_CLIENT_IDS ||
  'id.animo.paradym')
  .split(',')
  .map((s) => s.trim())
  .filter(Boolean);

// Legacy (WildFly) Keycloak serves everything under /auth. The deployed image
// is that generation — confirmed live at /auth/realms/sunbird-rc.
const AUTH = `${KC_BASE}/auth`;

const ROLE_STAFF = 'issuer-staff';
const ROLE_CITIZEN = 'citizen';
const FARMER_ID_CLAIM = 'farmerId';
const WALLET_SCOPE = 'farmer_credential';

/**
 * Login theme to select, e.g. `sunbird-issuer`. Empty leaves the realm's own
 * setting untouched — see ensureLoginTheme for why this is opt-in.
 */
const LOGIN_THEME = process.env.LOGIN_THEME || '';

/**
 * Where to discover the issuable credential types, so a Keycloak scope can exist
 * for each. Defaults alongside the gateway this script is already pointed at.
 */
const SCHEMA_CONFIGS_URL =
  process.env.SCHEMA_CONFIGS_URL || `${KC_BASE}/credential-schema/oid4vci-configs`;

// The wallet's redirect target. Credo-based wallets register a custom scheme;
// the concrete value must be read off the wallet build before this is usable in
// anger, which is why it is overridable and loudly defaulted.
const WALLET_REDIRECT_URIS = (
  process.env.WALLET_REDIRECT_URIS || 'openid4vci://callback,id.paradym.wallet://callback'
)
  .split(',')
  .map((s) => s.trim())
  .filter(Boolean);

// A vendor wallet's redirect is a real HTTPS URL it owns, not a custom scheme —
// Paradym round-trips through paradym.id and then back into the app.
const EXTERNAL_WALLET_REDIRECT_URIS = (
  process.env.EXTERNAL_WALLET_REDIRECT_URIS || 'https://paradym.id/invitation/redirect'
)
  .split(',')
  .map((s) => s.trim())
  .filter(Boolean);

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
  return fetch(`${AUTH}/admin/realms/${KC_REALM}${path}`, {
    method,
    headers: { authorization: `Bearer ${token}`, 'content-type': 'application/json' },
    body: body ? JSON.stringify(body) : undefined,
  });
}

async function kcJson(token, method, path, body) {
  const res = await kc(token, method, path, body);
  if (!res.ok) throw new Error(`${method} ${path} -> ${res.status}: ${await res.text()}`);
  const text = await res.text();
  return text ? JSON.parse(text) : undefined;
}

// --- realm -----------------------------------------------------------------

/**
 * Creates the realm if it is absent.
 *
 * The deployed Keycloak already had `sunbird-rc`, so this was initially assumed
 * — but the published image does NOT import it, so running this script against a
 * fresh Keycloak failed at the first client call with a bare 404. Creating it
 * here makes the script work from zero, which is what "idempotent" should mean.
 */
async function ensureRealm(token) {
  const res = await fetch(`${AUTH}/admin/realms/${KC_REALM}`, {
    headers: { authorization: `Bearer ${token}` },
  });
  if (res.ok) {
    console.log(`  ✓ realm ${KC_REALM} exists`);
    await ensureLoginTheme(token);
    return;
  }
  const created = await fetch(`${AUTH}/admin/realms`, {
    method: 'POST',
    headers: { authorization: `Bearer ${token}`, 'content-type': 'application/json' },
    body: JSON.stringify({
      realm: KC_REALM,
      enabled: true,
      displayName: 'Sunbird RC',
      // Holders sign in from a wallet on their own device; a 30-minute SSO idle
      // is long enough for a credential to be collected without leaving a
      // session open indefinitely.
      ssoSessionIdleTimeout: 1800,
      loginWithEmailAllowed: true,
      duplicateEmailsAllowed: false,
      // Registration stays off: accounts are provisioned by the issuing
      // authority, and self-registration would let anyone create a login that
      // simply has no record behind it.
      registrationAllowed: false,
    }),
  });
  if (!created.ok && created.status !== 409) {
    throw new Error(`create realm -> ${created.status}: ${await created.text()}`);
  }
  console.log(`  + realm ${KC_REALM} created`);
}

/**
 * Selects the branded sign-in theme, and supplies the brand lockup it styles.
 *
 * `displayNameHtml` carries that markup because the theme is CSS-only — see
 * keycloak-themes/sunbird-issuer/login/theme.properties for why no FreeMarker
 * template is overridden. It is the one hook Keycloak already renders as raw
 * markup in the login header.
 *
 * OPT-IN via LOGIN_THEME, deliberately. Naming a theme the server has not got
 * makes Keycloak fall back to the stock one and log a warning on every single
 * login, and there is no portable admin endpoint across Keycloak generations for
 * asking which themes are installed. So the caller — who knows whether the volume
 * is mounted — decides.
 */
async function ensureLoginTheme(token) {
  if (!LOGIN_THEME) {
    console.log('  · login theme not requested (set LOGIN_THEME to apply one)');
    return;
  }
  const realm = await kcJson(token, 'GET', '');
  const brand =
    '<span class="sb-brand"><span class="sb-mark">&#9672;</span>' +
    '<span class="sb-brand-text"><b>Sunbird RC</b><i>Issuer</i></span></span>';
  if (realm.loginTheme === LOGIN_THEME && realm.displayNameHtml === brand) {
    console.log(`  ✓ login theme ${LOGIN_THEME}`);
    return;
  }
  const res = await kc(token, 'PUT', '', {
    ...realm,
    loginTheme: LOGIN_THEME,
    displayNameHtml: brand,
  });
  console.log(
    res.ok
      ? `  + login theme ${LOGIN_THEME} selected`
      : `  ! could not set login theme (${res.status})`,
  );
}

// --- roles -----------------------------------------------------------------

async function ensureRealmRole(token, name, description) {
  const res = await kc(token, 'GET', `/roles/${encodeURIComponent(name)}`);
  if (res.ok) {
    console.log(`  ✓ role ${name} exists`);
    return;
  }
  const created = await kc(token, 'POST', '/roles', { name, description });
  if (!created.ok && created.status !== 409) {
    throw new Error(`create role ${name} -> ${created.status}: ${await created.text()}`);
  }
  console.log(`  + role ${name} created`);
}

// --- client scope carrying the farmerId claim ------------------------------

async function ensureFarmerIdScope(token) {
  const scopes = await kcJson(token, 'GET', '/client-scopes');
  let scope = scopes.find((s) => s.name === WALLET_SCOPE);
  const rep = {
    name: WALLET_SCOPE,
    description: 'Farmer Land Holding credential issuance; carries the farmerId claim',
    protocol: 'openid-connect',
    attributes: {
      'include.in.token.scope': 'true',
      'display.on.consent.screen': 'true',
      'consent.screen.text': 'Issue your Farmer Land Holding credential',
    },
  };
  if (!scope) {
    const res = await kc(token, 'POST', '/client-scopes', rep);
    if (!res.ok && res.status !== 409) {
      throw new Error(`create client-scope -> ${res.status}: ${await res.text()}`);
    }
    const after = await kcJson(token, 'GET', '/client-scopes');
    scope = after.find((s) => s.name === WALLET_SCOPE);
    console.log(`  + client scope ${WALLET_SCOPE} created`);
  } else {
    console.log(`  ✓ client scope ${WALLET_SCOPE} exists`);
  }

  // The mapper must emit into the access token as well as the ID token: the
  // wallet presents the ACCESS token at the credential endpoint, so an
  // ID-token-only claim would be invisible to oid4vc-service.
  // BOTH spellings get a mapper. The same concept is written `farmerId` by this
  // tooling and `farmer_id` by realms provisioned earlier, and a mapper only
  // emits a claim when the USER ATTRIBUTE name matches exactly. Providing one
  // spelling silently produced a token with no subject claim at all — which
  // surfaces as "your account is not linked to a record" for an account that is
  // linked, and cost three separate debugging rounds. Services read the claim
  // tolerantly; this makes the token side equally forgiving.
  const mappers = await kcJson(token, 'GET', `/client-scopes/${scope.id}/protocol-mappers/models`);
  for (const claim of [FARMER_ID_CLAIM, 'farmer_id']) {
    if (mappers.some((m) => m.name === claim)) {
      console.log(`  ✓ mapper ${claim} exists`);
      continue;
    }
    const res = await kc(token, 'POST', `/client-scopes/${scope.id}/protocol-mappers/models`, {
      name: claim,
      protocol: 'openid-connect',
      protocolMapper: 'oidc-usermodel-attribute-mapper',
      config: {
        'user.attribute': claim,
        'claim.name': claim,
        'jsonType.label': 'String',
        'id.token.claim': 'true',
        'access.token.claim': 'true',
        'userinfo.token.claim': 'true',
      },
    });
    if (!res.ok && res.status !== 409) {
      throw new Error(`create mapper ${claim} -> ${res.status}: ${await res.text()}`);
    }
    console.log(`  + mapper ${claim} created (id + access + userinfo)`);
  }
  return scope;
}

// --- clients ---------------------------------------------------------------

async function upsertClient(token, rep) {
  const list = await kcJson(token, 'GET', `/clients?clientId=${encodeURIComponent(rep.clientId)}`);
  if (list.length) {
    await kcJson(token, 'PUT', `/clients/${list[0].id}`, { ...list[0], ...rep });
    console.log(`  ✓ client ${rep.clientId} updated`);
    return list[0].id;
  }
  const res = await kc(token, 'POST', '/clients', rep);
  if (!res.ok && res.status !== 409) {
    throw new Error(`create client ${rep.clientId} -> ${res.status}: ${await res.text()}`);
  }
  const after = await kcJson(token, 'GET', `/clients?clientId=${encodeURIComponent(rep.clientId)}`);
  console.log(`  + client ${rep.clientId} created`);
  return after[0].id;
}

async function ensurePortalClient(token) {
  const redirect = `${PORTAL_PUBLIC_URL}${PORTAL_BASE_PATH}/callback`;
  // A wildcard over the portal's whole base path, not just /callback.
  //
  // Keycloak validates the LOGOUT redirect against this same list (with
  // `post.logout.redirect.uris: '+'` meaning "reuse redirectUris"). Registering
  // only /callback made sign-out fail with 400 "Invalid redirect uri", because
  // logout returns the user to the portal's landing page rather than to the
  // callback. The local session was cleared first, so it looked like logout
  // half-worked: the user landed on a Keycloak error page while actually being
  // signed out.
  const portalBase = `${PORTAL_PUBLIC_URL}${PORTAL_BASE_PATH}/*`;
  return upsertClient(token, {
    clientId: PORTAL_CLIENT_ID,
    name: 'Sunbird RC Issuer Portal',
    enabled: true,
    protocol: 'openid-connect',
    // Confidential: the code-for-token exchange happens in the BFF, never in
    // the browser, so the secret stays server-side.
    publicClient: false,
    secret: PORTAL_CLIENT_SECRET,
    standardFlowEnabled: true,
    directAccessGrantsEnabled: false,
    // MUST be true. The BFF obtains a service token via `client_credentials`
    // for two things: writing the `farmerId` attribute when staff link a login,
    // and reading a citizen's own record once the registry enforces roles (a
    // citizen token is refused there outright). With this false, that grant
    // returns 401 and the BFF surfaces it as a 502 — which is why "Link login"
    // never worked, despite looking wired up.
    serviceAccountsEnabled: true,
    redirectUris: [
      redirect,
      portalBase,
      // Local development outside the container stack: the BFF on its own port,
      // and the Vite dev server.
      'http://localhost:4100/issuer-portal/*',
      'http://localhost:5174/*',
    ],
    webOrigins: ['+'],
    // '+' means "reuse the registered redirect URIs", which now include the
    // portal's landing page via the wildcard above.
    attributes: { 'post.logout.redirect.uris': '+' },
  });
}

async function ensureWalletClient(token) {
  return upsertClient(token, {
    clientId: WALLET_CLIENT_ID,
    name: 'Sunbird Wallet (holder)',
    enabled: true,
    protocol: 'openid-connect',
    // Public + PKCE: a mobile wallet cannot keep a secret. `pkce.code.challenge.method`
    // set to S256 makes Keycloak REJECT a code exchange without a verifier,
    // rather than merely permitting one.
    publicClient: true,
    standardFlowEnabled: true,
    directAccessGrantsEnabled: false,
    redirectUris: WALLET_REDIRECT_URIS,
    attributes: {
      'pkce.code.challenge.method': 'S256',
      'post.logout.redirect.uris': '+',
    },
  });
}

/**
 * Public clients for wallets whose client id we do not control.
 *
 * Same shape as our own wallet client — public, PKCE-enforced, standard flow —
 * but with the wallet vendor's own redirect URI, which is a real HTTPS URL rather
 * than a custom scheme. Returns `{ clientId: uuid }` so the caller can attach the
 * credential scopes; without those a wallet is bounced with `invalid_scope` and
 * reports only "something went wrong".
 */
async function ensureExternalWalletClients(token) {
  const out = {};
  for (const clientId of EXTERNAL_WALLET_CLIENT_IDS) {
    out[clientId] = await upsertClient(token, {
      clientId,
      name: `External wallet (${clientId})`,
      enabled: true,
      protocol: 'openid-connect',
      publicClient: true,
      standardFlowEnabled: true,
      directAccessGrantsEnabled: false,
      redirectUris: EXTERNAL_WALLET_REDIRECT_URIS,
      attributes: {
        'pkce.code.challenge.method': 'S256',
        'post.logout.redirect.uris': '+',
      },
    });
  }
  return out;
}

/**
 * Grants the portal client's service account the roles the BFF needs:
 *
 *  - realm role `issuer-staff`, so the registry accepts its reads once
 *    `authentication_enabled` is on. Authorization is still decided by the BFF
 *    from the citizen's OWN token; this account is only the credential used to
 *    fetch the single record the BFF has already decided they may see.
 *  - realm-management `manage-users`, so linking a login can write the
 *    `farmerId` user attribute.
 *
 * Without these, `client_credentials` succeeds but every downstream call is
 * refused — a failure mode that looks like a network problem rather than a
 * missing grant.
 */
async function ensureServiceAccountRoles(token, clientUuid, clientLabel) {
  const sa = await kcJson(token, 'GET', `/clients/${clientUuid}/service-account-user`);
  if (!sa?.id) {
    console.log(`  ! ${clientLabel} has no service-account user; is serviceAccountsEnabled set?`);
    return;
  }

  const staffRole = await kcJson(token, 'GET', `/roles/${encodeURIComponent(ROLE_STAFF)}`);
  const realmRes = await kc(token, 'POST', `/users/${sa.id}/role-mappings/realm`, [staffRole]);
  if (!realmRes.ok && realmRes.status !== 409) {
    throw new Error(`grant ${ROLE_STAFF} to service account -> ${realmRes.status}`);
  }
  console.log(`  ✓ service account has ${ROLE_STAFF}`);

  // realm-management is a CLIENT role, so it needs the client's uuid and the
  // client-role mapping endpoint rather than the realm one.
  const [rm] = await kcJson(token, 'GET', '/clients?clientId=realm-management');
  if (!rm) {
    console.log('  ! realm-management client not found; skipping manage-users');
    return;
  }
  const manageUsers = await kcJson(
    token,
    'GET',
    `/clients/${rm.id}/roles/${encodeURIComponent('manage-users')}`,
  );
  const cRes = await kc(token, 'POST', `/users/${sa.id}/role-mappings/clients/${rm.id}`, [
    manageUsers,
  ]);
  if (!cRes.ok && cRes.status !== 409) {
    throw new Error(`grant manage-users to service account -> ${cRes.status}`);
  }
  console.log('  ✓ service account has realm-management manage-users');
}

async function attachScope(token, clientUuid, scopeId, clientLabel) {
  const res = await kc(token, 'PUT', `/clients/${clientUuid}/default-client-scopes/${scopeId}`);
  if (!res.ok && res.status !== 409) {
    throw new Error(`attach scope to ${clientLabel} -> ${res.status}: ${await res.text()}`);
  }
  console.log(`  ✓ scope ${WALLET_SCOPE} attached to ${clientLabel}`);
}


// --- force a fresh login for wallet clients --------------------------------

const FORCE_LOGIN_FLOW = 'wallet-force-login';

/**
 * Makes the wallet clients demand credentials on EVERY authorization request.
 *
 * Keycloak's built-in `browser` flow has the `auth-cookie` step as ALTERNATIVE,
 * so an existing SSO session satisfies authentication silently. A wallet keeps
 * its in-app browser session, so the second and later scans issued a credential
 * with no login page at all — the holder was never asked to prove who they were,
 * which defeats the point of the authorization_code flow.
 *
 * This copies the browser flow, disables the Cookie step in the copy, and binds
 * it to the wallet clients ONLY, via authenticationFlowBindingOverrides. Staff
 * signing into the portal keep normal SSO, because that is a different client.
 *
 * Prefer this over shortening the realm's SSO lifetimes: those are realm-wide
 * and time-based, so they would degrade every other login and still leave a
 * window in which a scan skips the prompt.
 */
async function ensureForceLoginFlow(token) {
  const flows = await kcJson(token, 'GET', '/authentication/flows');
  if (!flows.some((f) => f.alias === FORCE_LOGIN_FLOW)) {
    const res = await kc(token, 'POST', '/authentication/flows/browser/copy', {
      newName: FORCE_LOGIN_FLOW,
    });
    if (!res.ok && res.status !== 409) {
      throw new Error(`copy browser flow -> ${res.status}: ${await res.text()}`);
    }
    console.log(`  + flow ${FORCE_LOGIN_FLOW} created`);
  } else {
    console.log(`  ✓ flow ${FORCE_LOGIN_FLOW} exists`);
  }

  const execs = await kcJson(
    token,
    'GET',
    `/authentication/flows/${FORCE_LOGIN_FLOW}/executions`,
  );
  const cookie = execs.find((e) => e.providerId === 'auth-cookie');
  if (!cookie) {
    console.log('  ! no auth-cookie step to disable — check the flow by hand');
  } else if (cookie.requirement === 'DISABLED') {
    console.log('  ✓ Cookie step already disabled');
  } else {
    cookie.requirement = 'DISABLED';
    await kcJson(token, 'PUT', `/authentication/flows/${FORCE_LOGIN_FLOW}/executions`, cookie);
    console.log('  + Cookie step disabled (no silent SSO)');
  }

  const after = await kcJson(token, 'GET', '/authentication/flows');
  return after.find((f) => f.alias === FORCE_LOGIN_FLOW)?.id;
}

/** Binds the force-login flow to one client, leaving other clients alone. */
async function bindForceLogin(token, clientId, flowId) {
  if (!flowId) return;
  const list = await kcJson(token, 'GET', `/clients?clientId=${encodeURIComponent(clientId)}`);
  if (!list.length) {
    console.log(`  ! client ${clientId} not found; skipping force-login binding`);
    return;
  }
  const client = list[0];
  await kcJson(token, 'PUT', `/clients/${client.id}`, {
    ...client,
    authenticationFlowBindingOverrides: { browser: flowId },
  });
  console.log(`  ✓ ${clientId} must re-authenticate on every scan`);
}

// --- users -----------------------------------------------------------------

const PASSWORD = process.env.SAMPLE_USER_PASSWORD || 'Passw0rd!';

async function ensureUser(token, u) {
  const list = await kcJson(token, 'GET', `/users?username=${encodeURIComponent(u.username)}&exact=true`);
  const attributes = Object.fromEntries(
    Object.entries(u.attrs || {}).map(([k, v]) => [k, [String(v)]]),
  );
  const rep = {
    username: u.username,
    enabled: true,
    firstName: u.fullName.split(' ')[0],
    lastName: u.fullName.split(' ').slice(1).join(' ') || '.',
    email: `${u.username}@example.gov`,
    emailVerified: true,
    attributes,
  };
  let id;
  if (list.length) {
    id = list[0].id;
    await kcJson(token, 'PUT', `/users/${id}`, { ...list[0], ...rep });
    console.log(`  ✓ user ${u.username} updated`);
  } else {
    const res = await kc(token, 'POST', '/users', rep);
    if (!res.ok) throw new Error(`create user ${u.username} -> ${res.status}: ${await res.text()}`);
    const created = await kcJson(token, 'GET', `/users?username=${encodeURIComponent(u.username)}&exact=true`);
    id = created[0].id;
    console.log(`  + user ${u.username} created`);
  }
  await kcJson(token, 'PUT', `/users/${id}/reset-password`, {
    type: 'password',
    value: PASSWORD,
    temporary: false,
  });

  // Realm role assignment needs the role's full representation, not just its
  // name — Keycloak silently no-ops on a partial body.
  const role = await kcJson(token, 'GET', `/roles/${encodeURIComponent(u.role)}`);
  const res = await kc(token, 'POST', `/users/${id}/role-mappings/realm`, [role]);
  if (!res.ok && res.status !== 409) {
    throw new Error(`assign ${u.role} to ${u.username} -> ${res.status}: ${await res.text()}`);
  }
  console.log(`      role ${u.role}${u.attrs?.farmerId ? `, farmerId ${u.attrs.farmerId}` : ''}`);
  return id;
}

// --- one OAuth scope per issuable credential type ---------------------------

/** Mirrors oid4vc-service's vct.util.ts slugifyVct(). */
const slugify = (name) =>
  String(name)
    .normalize('NFKD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');

/**
 * Registers a Keycloak client scope for every issuable credential type, and makes
 * it requestable by the wallet clients.
 *
 * REQUIRED, not cosmetic. A wallet running authorization_code asks Keycloak for
 * `scope=<slug of the credential name>` (oid4vci.service.ts). Keycloak refuses a
 * scope the client cannot request, and the refusal arrives as an immediate
 * redirect to the wallet's callback with `error=invalid_scope` — so the holder
 * never sees a login page and the wallet reports only "something went wrong".
 *
 * Discovered from the schema registry rather than hard-coded, so adding a
 * credential type needs no edit here: re-run this script and its scope appears.
 */
async function ensureCredentialScopes(token, clientUuids) {
  let configs;
  try {
    const res = await fetch(SCHEMA_CONFIGS_URL);
    if (!res.ok) throw new Error(`${res.status}`);
    configs = await res.json();
  } catch (e) {
    // Not fatal: on a first run the schemas do not exist yet. The bootstrap
    // re-runs this step after creating them.
    console.log(`  ! could not read ${SCHEMA_CONFIGS_URL} (${e.message}) — skipping scope sync`);
    return;
  }

  const names = [
    ...new Set(
      configs
        .filter((c) => (c.formats ?? []).includes('vc+sd-jwt'))
        .map((c) => c.name)
        .filter(Boolean),
    ),
  ];
  if (!names.length) {
    console.log('  ! no vc+sd-jwt credential types published yet — skipping scope sync');
    return;
  }

  const existing = await kcJson(token, 'GET', '/client-scopes');
  for (const name of names) {
    const slug = slugify(name);
    let scope = existing.find((s) => s.name === slug);
    if (!scope) {
      const res = await kc(token, 'POST', '/client-scopes', {
        name: slug,
        description: `Issuance of the ${name}`,
        protocol: 'openid-connect',
        attributes: {
          'include.in.token.scope': 'true',
          'display.on.consent.screen': 'true',
          'consent.screen.text': `Issue your ${name}`,
        },
      });
      if (!res.ok && res.status !== 409) {
        throw new Error(`create client-scope ${slug} -> ${res.status}: ${await res.text()}`);
      }
      const after = await kcJson(token, 'GET', '/client-scopes');
      scope = after.find((s) => s.name === slug);
      console.log(`  + client scope ${slug} created`);
    } else {
      console.log(`  ✓ client scope ${slug} exists`);
    }
    if (!scope) continue;

    // OPTIONAL, not default: the wallet names the one credential type it is
    // collecting. A default scope would put every type in every token.
    for (const [label, uuid] of Object.entries(clientUuids)) {
      const res = await kc(token, 'PUT', `/clients/${uuid}/optional-client-scopes/${scope.id}`);
      if (!res.ok && res.status !== 409) {
        console.log(`  ! could not attach ${slug} to ${label} (${res.status})`);
      }
    }
  }
  console.log(`  ✓ ${names.length} credential scope(s) requestable by the wallet clients`);
}

// --- main ------------------------------------------------------------------

async function main() {
  console.log(`Configuring Keycloak at ${AUTH} (realm ${KC_REALM})`);
  const token = await adminToken();

  console.log('\nRealm');
  await ensureRealm(token);

  console.log('\nRoles');
  await ensureRealmRole(token, ROLE_STAFF, 'Issuer portal staff: registry CRUD and credential issuance');
  await ensureRealmRole(token, ROLE_CITIZEN, 'Credential holder: self-service issuance of own credentials only');

  console.log('\nClient scope');
  const scope = await ensureFarmerIdScope(token);

  console.log('\nClients');
  const portalUuid = await ensurePortalClient(token);
  const walletUuid = await ensureWalletClient(token);
  const externalWallets = await ensureExternalWalletClients(token);
  await attachScope(token, portalUuid, scope.id, PORTAL_CLIENT_ID);
  await attachScope(token, walletUuid, scope.id, WALLET_CLIENT_ID);
  for (const [id, uuid] of Object.entries(externalWallets)) {
    await attachScope(token, uuid, scope.id, id);
  }
  await ensureServiceAccountRoles(token, portalUuid, PORTAL_CLIENT_ID);

  console.log('\nCredential scopes (one per issuable type)');
  await ensureCredentialScopes(token, {
    [WALLET_CLIENT_ID]: walletUuid,
    [PORTAL_CLIENT_ID]: portalUuid,
    ...externalWallets,
  });

  console.log('\nForce fresh login for wallets');
  const forceFlowId = await ensureForceLoginFlow(token);
  // Only the wallet clients: staff keep normal SSO in the portal.
  for (const c of [WALLET_CLIENT_ID, ...EXTERNAL_WALLET_CLIENT_IDS]) {
    await bindForceLogin(token, c, forceFlowId);
  }

  console.log('\nUsers');
  await ensureUser(token, {
    username: 'issuer.staff',
    fullName: 'Meera Iyer',
    role: ROLE_STAFF,
  });
  // Citizens carry the farmerId that links their login to a registry record.
  // Seed the matching Farmer/LandParcel rows with scripts/seed-registry.mjs.
  await ensureUser(token, {
    username: 'farmer.ravi',
    fullName: 'Ravi Kumar',
    role: ROLE_CITIZEN,
    attrs: { farmerId: 'FRM-000123' },
  });
  await ensureUser(token, {
    username: 'farmer.lakshmi',
    fullName: 'Lakshmi Devi',
    role: ROLE_CITIZEN,
    attrs: { farmerId: 'FRM-000124' },
  });
  // Holders of the other two issuers. The claim is `farmerId` for every issuer
  // regardless of what its holders are called: it is the registry key, and the
  // whole self-service chain (this attribute -> access token claim -> registry
  // lookup) is keyed on that one name. Renaming it per issuer would mean a
  // Keycloak mapper, a token claim and a service lookup per domain.
  await ensureUser(token, {
    username: 'citizen.meera',
    fullName: 'Meera Nair',
    role: ROLE_CITIZEN,
    attrs: { farmerId: 'AGE-000001' },
  });
  await ensureUser(token, {
    username: 'student.priya',
    fullName: 'Priya Sharma',
    role: ROLE_CITIZEN,
    attrs: { farmerId: 'EDU-000001' },
  });
  // Deliberately has NO farmerId: exercises the "authenticated but no record"
  // path, which must fail cleanly rather than with an opaque 500.
  await ensureUser(token, {
    username: 'farmer.norecord',
    fullName: 'Unlinked Citizen',
    role: ROLE_CITIZEN,
  });

  console.log(`\nDone. Sample users share the password: ${PASSWORD}`);
  console.log(`  staff:    issuer.staff        -> ${PORTAL_PUBLIC_URL}${PORTAL_BASE_PATH}`);
  console.log('  holders:  farmer.ravi (FRM-000123), farmer.lakshmi (FRM-000124)');
  console.log('            citizen.meera (AGE-000001), student.priya (EDU-000001)');
  console.log('            farmer.norecord (no farmerId — negative test)');
  if (!process.env.WALLET_REDIRECT_URIS) {
    console.log(
      `\n  NOTE: ${WALLET_CLIENT_ID} redirect URIs are placeholders ` +
        `(${WALLET_REDIRECT_URIS.join(', ')}).\n` +
        '        Re-run with WALLET_REDIRECT_URIS set to the wallet build\'s real scheme.',
    );
  }
}

main().catch((e) => {
  console.error('KEYCLOAK SETUP FAILED:', e.message);
  process.exit(1);
});
