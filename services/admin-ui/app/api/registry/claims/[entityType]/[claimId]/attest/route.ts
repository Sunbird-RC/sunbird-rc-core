import { NextRequest, NextResponse } from 'next/server'
import { attestClaim } from '@/lib/server/clients/registryClient'
import { ApiError } from '@/lib/server/http'

export async function POST(req: NextRequest, { params }: { params: { entityType: string; claimId: string } }) {
  const { action, notes } = await req.json()
  try {
    return NextResponse.json(await attestClaim(params.entityType, params.claimId, action, notes))
  } catch (e) {
    if (e instanceof ApiError) return NextResponse.json({ message: e.message }, { status: e.status })
    throw e
  }
}
