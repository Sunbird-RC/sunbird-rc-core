import { NextRequest, NextResponse } from 'next/server'
import { getEntriesSince } from '@/lib/server/inspector'
import { buildCurl, DEFAULT_HOST_MAP, parseHostMapOverride, rewriteUrl } from '@/lib/shared/curl'

export const dynamic = 'force-dynamic'

export async function GET(req: NextRequest) {
  const since = Number(req.nextUrl.searchParams.get('since') ?? '0')
  const hostMap = { ...DEFAULT_HOST_MAP, ...parseHostMapOverride(process.env.INSPECTOR_PUBLIC_BASES) }

  const entries = getEntriesSince(Number.isFinite(since) ? since : 0).map((e) => ({
    ...e,
    publicUrl: rewriteUrl(e.url, hostMap),
    curl: buildCurl(e, hostMap),
  }))

  return NextResponse.json(entries)
}
