import { NextRequest, NextResponse } from 'next/server'
import { listClaims } from '@/lib/server/clients/registryClient'
import { ApiError } from '@/lib/server/http'

export async function GET(_req: NextRequest, { params }: { params: { entityType: string } }) {
  try {
    return NextResponse.json(await listClaims(params.entityType))
  } catch (e) {
    if (e instanceof ApiError) return NextResponse.json({ message: e.message }, { status: e.status })
    throw e
  }
}
