import { NextRequest, NextResponse } from 'next/server'
import { authorizeUrl } from '@/lib/server/oidc'
import { randomUUID } from 'crypto'

export async function GET(req: NextRequest) {
  const state = randomUUID()
  const redirectUri = new URL('/admin/api/auth/callback', req.url).toString()
  const res = NextResponse.redirect(authorizeUrl(state, redirectUri))
  res.cookies.set('admin_ui_oauth_state', state, { httpOnly: true, path: '/admin', maxAge: 300 })
  return res
}
