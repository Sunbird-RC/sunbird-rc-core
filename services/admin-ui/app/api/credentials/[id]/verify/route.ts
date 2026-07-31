import { NextRequest, NextResponse } from 'next/server'
import { verifyCredential } from '@/lib/server/clients/credentialsClient'
import { ApiError } from '@/lib/server/http'

export async function GET(_req: NextRequest, { params }: { params: { id: string } }) {
  try {
    return NextResponse.json(await verifyCredential(params.id))
  } catch (e) {
    if (e instanceof ApiError) return NextResponse.json({ message: e.message }, { status: e.status })
    throw e
  }
}
