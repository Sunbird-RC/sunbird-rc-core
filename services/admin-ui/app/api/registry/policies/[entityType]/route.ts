import { NextRequest, NextResponse } from 'next/server'
import { createPolicy, listPolicies } from '@/lib/server/clients/registryClient'
import { ApiError } from '@/lib/server/http'

export async function GET(_req: NextRequest, { params }: { params: { entityType: string } }) {
  try {
    return NextResponse.json(await listPolicies(params.entityType))
  } catch (e) {
    if (e instanceof ApiError) return NextResponse.json({ message: e.message }, { status: e.status })
    throw e
  }
}

export async function POST(req: NextRequest, { params }: { params: { entityType: string } }) {
  const body = await req.json()
  try {
    return NextResponse.json(await createPolicy(params.entityType, body))
  } catch (e) {
    if (e instanceof ApiError) return NextResponse.json({ message: e.message }, { status: e.status })
    throw e
  }
}
