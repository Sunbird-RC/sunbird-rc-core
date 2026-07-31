import { NextRequest, NextResponse } from 'next/server'
import { endSessionUrl } from '@/lib/server/oidc'
import { clearSessionCookie } from '@/lib/server/session'

export async function GET(req: NextRequest) {
  const postLogoutRedirectUri = new URL('/admin/login', req.url).toString()
  const res = NextResponse.redirect(endSessionUrl(postLogoutRedirectUri))
  clearSessionCookie(res)
  return res
}
