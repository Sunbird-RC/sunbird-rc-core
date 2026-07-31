import { NextRequest, NextResponse } from 'next/server'
import { searchAudit } from '@/lib/server/clients/registryClient'
import { ApiError } from '@/lib/server/http'

export async function GET(req: NextRequest) {
  const q = req.nextUrl.searchParams.get('q') ?? undefined
  try {
    const events = await searchAudit(q ? { text: q } : {})
    return NextResponse.json(events)
  } catch (e) {
    if (e instanceof ApiError) return NextResponse.json({ message: e.message }, { status: e.status })
    throw e
  }
}
