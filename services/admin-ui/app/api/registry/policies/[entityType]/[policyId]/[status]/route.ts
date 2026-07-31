import { NextRequest, NextResponse } from 'next/server'
import { togglePolicyStatus } from '@/lib/server/clients/registryClient'
import { ApiError } from '@/lib/server/http'

export async function PUT(
  _req: NextRequest,
  { params }: { params: { entityType: string; policyId: string; status: string } },
) {
  try {
    return NextResponse.json(
      await togglePolicyStatus(params.entityType, params.policyId, params.status as 'DRAFT' | 'PUBLISHED'),
    )
  } catch (e) {
    if (e instanceof ApiError) return NextResponse.json({ message: e.message }, { status: e.status })
    throw e
  }
}
