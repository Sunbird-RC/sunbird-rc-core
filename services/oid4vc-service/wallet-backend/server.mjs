// Wallet backend: production-like accounts + persistence for the browser wallet.
//   - signup/login (bcrypt password hashes, JWT sessions)
//   - per-user holder key (stored ENCRYPTED — server never sees plaintext)
//   - per-user credential store (CRUD)
// All routes are mounted under /wallet-api so the same paths work directly and
// behind nginx (which proxies /wallet-api through unchanged).
import express from 'express';
import cors from 'cors';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import pg from 'pg';

const PORT = process.env.PORT || 4100;
const JWT_SECRET = process.env.WALLET_JWT_SECRET || 'dev-wallet-secret-change-me';
const PGHOST = process.env.PGHOST || 'db';
const PGPORT = process.env.PGPORT || 5432;
const PGUSER = process.env.PGUSER || 'postgres';
const PGPASSWORD = process.env.PGPASSWORD || 'postgres';
const WALLET_DB = process.env.WALLET_DB || 'wallet';

// Trusted OID4VP verifier allowlist, e.g.:
//   WALLET_TRUSTED_VERIFIERS='[{"clientId":"did:web:issuer.example","responseUriOrigin":"https://issuer.example"}]'
// Parsed once at startup rather than per-request — an operator error here
// (malformed JSON) should fail loudly at boot, not silently disable the
// allowlist. See test-wallet/src/trust.mjs for how entries are matched.
let TRUSTED_VERIFIERS = null;
if (process.env.WALLET_TRUSTED_VERIFIERS) {
  TRUSTED_VERIFIERS = JSON.parse(process.env.WALLET_TRUSTED_VERIFIERS);
}

let pool;

// Create the wallet DB (via the maintenance 'postgres' db) + tables, idempotent.
async function initDb() {
  const admin = new pg.Client({ host: PGHOST, port: PGPORT, user: PGUSER, password: PGPASSWORD, database: 'postgres' });
  await admin.connect();
  try {
    await admin.query(`CREATE DATABASE ${WALLET_DB}`);
    console.log(`created database ${WALLET_DB}`);
  } catch (e) {
    if (e.code !== '42P04') throw e; // 42P04 = duplicate_database
  }
  await admin.end();

  pool = new pg.Pool({ host: PGHOST, port: PGPORT, user: PGUSER, password: PGPASSWORD, database: WALLET_DB });
  await pool.query(`
    CREATE TABLE IF NOT EXISTS wallet_users (
      id SERIAL PRIMARY KEY,
      username TEXT UNIQUE NOT NULL,
      password_hash TEXT NOT NULL,
      created_at TIMESTAMPTZ DEFAULT now()
    );
    CREATE TABLE IF NOT EXISTS wallet_holder (
      user_id INTEGER PRIMARY KEY REFERENCES wallet_users(id) ON DELETE CASCADE,
      enc_holder TEXT NOT NULL,
      did TEXT NOT NULL,
      created_at TIMESTAMPTZ DEFAULT now()
    );
    CREATE TABLE IF NOT EXISTS wallet_credentials (
      id SERIAL PRIMARY KEY,
      user_id INTEGER NOT NULL REFERENCES wallet_users(id) ON DELETE CASCADE,
      record JSONB NOT NULL,
      format TEXT,
      type TEXT,
      issuer TEXT,
      created_at TIMESTAMPTZ DEFAULT now()
    );
  `);
  console.log('wallet tables ready');
}

const app = express();
app.use(cors());
app.use(express.json({ limit: '2mb' }));

function sign(user) { return jwt.sign({ uid: user.id, username: user.username }, JWT_SECRET, { expiresIn: '7d' }); }
function auth(req, res, next) {
  const h = req.headers.authorization || '';
  const t = h.startsWith('Bearer ') ? h.slice(7) : null;
  if (!t) return res.status(401).json({ error: 'missing token' });
  try { req.user = jwt.verify(t, JWT_SECRET); next(); }
  catch { return res.status(401).json({ error: 'invalid token' }); }
}

app.get('/wallet-api/health', (_req, res) => res.json({ ok: true }));

// Public (no auth) — the browser wallet fetches this once at startup to
// configure test-wallet/src/trust.mjs's isTrustedVerifier(). `null` means
// "not configured", which the wallet treats as allow-all-with-a-warning
// rather than deny-all, so an operator who forgets this env var gets a
// working (if unsafe) demo instead of a wallet that can't present anything.
app.get('/wallet-api/trusted-verifiers', (_req, res) => res.json(TRUSTED_VERIFIERS));

app.post('/wallet-api/signup', async (req, res) => {
  const { username, password } = req.body || {};
  if (!username || !password) return res.status(400).json({ error: 'username and password required' });
  try {
    const hash = await bcrypt.hash(password, 10);
    const r = await pool.query('INSERT INTO wallet_users(username, password_hash) VALUES($1,$2) RETURNING id, username', [username.toLowerCase().trim(), hash]);
    res.json({ token: sign(r.rows[0]), username: r.rows[0].username });
  } catch (e) {
    if (e.code === '23505') return res.status(409).json({ error: 'username already taken' });
    res.status(500).json({ error: e.message });
  }
});

app.post('/wallet-api/login', async (req, res) => {
  const { username, password } = req.body || {};
  if (!username || !password) return res.status(400).json({ error: 'username and password required' });
  const r = await pool.query('SELECT * FROM wallet_users WHERE username=$1', [username.toLowerCase().trim()]);
  const u = r.rows[0];
  if (!u || !(await bcrypt.compare(password, u.password_hash))) return res.status(401).json({ error: 'invalid credentials' });
  res.json({ token: sign(u), username: u.username });
});

// Encrypted holder key (server stores ciphertext only) --------------------
app.get('/wallet-api/holder', auth, async (req, res) => {
  const r = await pool.query('SELECT enc_holder, did FROM wallet_holder WHERE user_id=$1', [req.user.uid]);
  res.json(r.rows[0] || { enc_holder: null, did: null });
});
app.put('/wallet-api/holder', auth, async (req, res) => {
  const { enc_holder, did } = req.body || {};
  if (!enc_holder || !did) return res.status(400).json({ error: 'enc_holder and did required' });
  await pool.query(
    `INSERT INTO wallet_holder(user_id, enc_holder, did) VALUES($1,$2,$3)
     ON CONFLICT (user_id) DO UPDATE SET enc_holder=$2, did=$3`,
    [req.user.uid, enc_holder, did],
  );
  res.json({ ok: true });
});
app.delete('/wallet-api/holder', auth, async (req, res) => {
  // Reset identity: drop holder + all stored credentials for this user.
  await pool.query('DELETE FROM wallet_credentials WHERE user_id=$1', [req.user.uid]);
  await pool.query('DELETE FROM wallet_holder WHERE user_id=$1', [req.user.uid]);
  res.json({ ok: true });
});

// Credential store --------------------------------------------------------
app.get('/wallet-api/credentials', auth, async (req, res) => {
  const r = await pool.query('SELECT id, record FROM wallet_credentials WHERE user_id=$1 ORDER BY id', [req.user.uid]);
  res.json(r.rows.map((row) => ({ ...row.record, id: row.id })));
});
app.post('/wallet-api/credentials', auth, async (req, res) => {
  const rec = req.body || {};
  const r = await pool.query(
    'INSERT INTO wallet_credentials(user_id, record, format, type, issuer) VALUES($1,$2,$3,$4,$5) RETURNING id',
    [req.user.uid, rec, rec.format || null, rec.vct || rec.docType || rec.configId || null, rec.issuer || null],
  );
  res.json({ ...rec, id: r.rows[0].id });
});
app.delete('/wallet-api/credentials/:id', auth, async (req, res) => {
  await pool.query('DELETE FROM wallet_credentials WHERE user_id=$1 AND id=$2', [req.user.uid, req.params.id]);
  res.json({ ok: true });
});

initDb()
  .then(() => app.listen(PORT, () => console.log(`wallet-backend on :${PORT}`)))
  .catch((e) => { console.error('DB init failed:', e.message); process.exit(1); });
