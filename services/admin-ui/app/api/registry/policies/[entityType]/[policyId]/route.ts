import { NextRequest, NextResponse } from 'next/server'
import { deletePolicy } from '@/lib/server/clients/registryClient'
import { ApiError } from '@/lib/server/http'

export async function DELETE(_req: NextRequest, { params }: { params: { entityType: string; policyId: string } }) {
  try {
    return NextResponse.json(await deletePolicy(params.entityType, params.policyId))
  } catch (e) {
    if (e instanceof ApiError) return NextResponse.json({ message: e.message }, { status: e.status })
    throw e
  }
}
