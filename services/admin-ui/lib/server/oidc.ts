import 'server-only'

// Minimal hand-rolled OIDC auth-code client — deliberately not next-auth.
// The BFF terminates the flow itself and only ever stores the session
// server-side (see session.ts), so there's no benefit to pulling in a
// larger framework for this one flow.
function issuer(): string {
  const issuer = process.env.KEYCLOAK_ISSUER
  if (!issuer) throw new Error('Missing required env var: KEYCLOAK_ISSUER')
  return issuer
}

export function authorizeUrl(state: string, redirectUri: string): string {
  const url = new URL(`${issuer()}/protocol/openid-connect/auth`)
  url.searchParams.set('client_id', 'admin-ui')
  url.searchParams.set('response_type', 'code')
  url.searchParams.set('scope', 'openid profile email')
  url.searchParams.set('redirect_uri', redirectUri)
  url.searchParams.set('state', state)
  return url.toString()
}

export function endSessionUrl(postLogoutRedirectUri: string): string {
  const url = new URL(`${issuer()}/protocol/openid-connect/logout`)
  url.searchParams.set('post_logout_redirect_uri', postLogoutRedirectUri)
  return url.toString()
}

type TokenResponse = {
  access_token: string
  id_token: string
  refresh_token?: string
}

export async function exchangeCode(code: string, redirectUri: string): Promise<TokenResponse> {
  const clientSecret = process.env.KEYCLOAK_ADMIN_UI_CLIENT_SECRET
  if (!clientSecret) throw new Error('Missing required env var: KEYCLOAK_ADMIN_UI_CLIENT_SECRET')

  const res = await fetch(`${issuer()}/protocol/openid-connect/token`, {
    method: 'POST',
    headers: { 'content-type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'authorization_code',
      client_id: 'admin-ui',
      client_secret: clientSecret,
      code,
      redirect_uri: redirectUri,
    }),
  })
  if (!res.ok) throw new Error(`Token exchange failed: ${res.status} — ${await res.text()}`)
  return res.json() as Promise<TokenResponse>
}

export function decodeIdTokenClaims(idToken: string): {
  sub: string
  name?: string
  preferred_username?: string
  email?: string
  realm_access?: { roles?: string[] }
} {
  const payload = idToken.split('.')[1]
  return JSON.parse(Buffer.from(payload, 'base64url').toString('utf8'))
}
