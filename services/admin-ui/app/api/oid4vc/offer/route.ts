import { NextRequest, NextResponse } from 'next/server'
import { createOffer } from '@/lib/server/clients/oid4vcClient'
import { ApiError } from '@/lib/server/http'

export async function POST(req: NextRequest) {
  const body = await req.json()
  try {
    return NextResponse.json(await createOffer(body))
  } catch (e) {
    if (e instanceof ApiError) return NextResponse.json({ message: e.message }, { status: e.status })
    throw e
  }
}
