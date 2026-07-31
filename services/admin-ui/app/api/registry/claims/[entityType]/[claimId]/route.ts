import { NextRequest, NextResponse } from 'next/server'
import { getClaim } from '@/lib/server/clients/registryClient'
import { ApiError } from '@/lib/server/http'

export async function GET(_req: NextRequest, { params }: { params: { entityType: string; claimId: string } }) {
  try {
    return NextResponse.json(await getClaim(params.entityType, params.claimId))
  } catch (e) {
    if (e instanceof ApiError) return NextResponse.json({ message: e.message }, { status: e.status })
    throw e
  }
}
