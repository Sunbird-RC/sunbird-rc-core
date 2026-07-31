import { NextRequest, NextResponse } from 'next/server'
import { decodeIdTokenClaims, exchangeCode } from '@/lib/server/oidc'
import { createSessionCookie, setSessionCookie } from '@/lib/server/session'

export async function GET(req: NextRequest) {
  const url = new URL(req.url)
  const code = url.searchParams.get('code')
  const state = url.searchParams.get('state')
  const expectedState = req.cookies.get('admin_ui_oauth_state')?.value

  if (!code || !state || state !== expectedState) {
    return NextResponse.redirect(new URL('/admin/login?error=state_mismatch', req.url))
  }

  const redirectUri = new URL('/admin/api/auth/callback', req.url).toString()
  const tokens = await exchangeCode(code, redirectUri)
  const claims = decodeIdTokenClaims(tokens.id_token)

  const sessionToken = await createSessionCookie({
    sub: claims.sub,
    name: claims.name ?? claims.preferred_username ?? claims.sub,
    email: claims.email,
    roles: claims.realm_access?.roles ?? [],
    accessToken: tokens.access_token,
  })

  const res = NextResponse.redirect(new URL('/admin/entities', req.url))
  res.cookies.delete('admin_ui_oauth_state')
  setSessionCookie(res, sessionToken)
  return res
}
