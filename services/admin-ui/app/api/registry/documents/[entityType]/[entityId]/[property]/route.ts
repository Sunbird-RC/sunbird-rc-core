import { NextRequest, NextResponse } from 'next/server'
import { listDocuments } from '@/lib/server/clients/registryClient'
import { ApiError } from '@/lib/server/http'

export async function GET(
  _req: NextRequest,
  { params }: { params: { entityType: string; entityId: string; property: string } },
) {
  try {
    return NextResponse.json(await listDocuments(params.entityType, params.entityId, params.property))
  } catch (e) {
    if (e instanceof ApiError) return NextResponse.json({ message: e.message }, { status: e.status })
    throw e
  }
}
