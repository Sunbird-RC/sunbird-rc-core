import { NextRequest, NextResponse } from 'next/server'
import { jwtVerify } from 'jose'

// Guards every (console) page and every /api/* route handler except the
// auth flow itself and /api/config (needed by the login page's own flag
// read, and safe to expose — it's read-only, no secrets). ADMIN_UI_AUTH_ENABLED
// is a local-dev bypass, mirroring the backend flag conventions in env.ts.
const PUBLIC_PATHS = ['/login', '/api/auth', '/api/config']

export async function middleware(req: NextRequest) {
  if (process.env.ADMIN_UI_AUTH_ENABLED === 'false') return NextResponse.next()

  const path = req.nextUrl.pathname.replace(/^\/admin/, '')
  if (PUBLIC_PATHS.some((p) => path.startsWith(p))) return NextResponse.next()

  const token = req.cookies.get('admin_ui_session')?.value
  if (!token) return NextResponse.redirect(new URL('/admin/login', req.url))

  try {
    const secret = process.env.SESSION_SECRET
    if (!secret) throw new Error('Missing SESSION_SECRET')
    await jwtVerify(token, new TextEncoder().encode(secret))
    return NextResponse.next()
  } catch {
    return NextResponse.redirect(new URL('/admin/login', req.url))
  }
}

export const config = {
  matcher: ['/((?!_next/static|_next/image|favicon.ico).*)'],
}
