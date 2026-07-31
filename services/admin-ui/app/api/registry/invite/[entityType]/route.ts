import { NextRequest, NextResponse } from 'next/server'
import { sendInvite } from '@/lib/server/clients/registryClient'
import { ApiError } from '@/lib/server/http'

export async function POST(req: NextRequest, { params }: { params: { entityType: string } }) {
  const body = await req.json()
  try {
    return NextResponse.json(await sendInvite(params.entityType, body))
  } catch (e) {
    if (e instanceof ApiError) return NextResponse.json({ message: e.message }, { status: e.status })
    throw e
  }
}
