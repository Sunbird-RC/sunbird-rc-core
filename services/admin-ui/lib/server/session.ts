import 'server-only'
import { SignJWT, jwtVerify } from 'jose'
import { cookies } from 'next/headers'

const COOKIE_NAME = 'admin_ui_session'
const SESSION_TTL_SECONDS = 60 * 60 * 8 // 8h — matches a typical operator shift

function secretKey(): Uint8Array {
  const secret = process.env.SESSION_SECRET
  if (!secret) throw new Error('Missing required env var: SESSION_SECRET')
  return new TextEncoder().encode(secret)
}

export type Session = {
  sub: string
  name: string
  email?: string
  roles: string[]
  // Kept for Phase 7 (Tier-2 bearer forwarding) — never sent to the browser,
  // this whole object lives only in the signed httpOnly cookie.
  accessToken: string
}

export async function createSessionCookie(session: Session): Promise<string> {
  return new SignJWT({ ...session })
    .setProtectedHeader({ alg: 'HS256' })
    .setIssuedAt()
    .setExpirationTime(`${SESSION_TTL_SECONDS}s`)
    .sign(secretKey())
}

export async function getSession(): Promise<Session | null> {
  const raw = cookies().get(COOKIE_NAME)?.value
  if (!raw) return null
  try {
    const { payload } = await jwtVerify(raw, secretKey())
    return payload as unknown as Session
  } catch {
    return null
  }
}

export async function requireSession(): Promise<Session> {
  const session = await getSession()
  if (!session) throw new Error('Unauthenticated')
  return session
}

export function setSessionCookie(response: Response, token: string) {
  response.headers.append(
    'Set-Cookie',
    `${COOKIE_NAME}=${token}; Path=/admin; HttpOnly; SameSite=Lax; Max-Age=${SESSION_TTL_SECONDS}${
      process.env.NODE_ENV === 'production' ? '; Secure' : ''
    }`,
  )
}

export function clearSessionCookie(response: Response) {
  response.headers.append('Set-Cookie', `${COOKIE_NAME}=; Path=/admin; HttpOnly; Max-Age=0`)
}

export { COOKIE_NAME }
