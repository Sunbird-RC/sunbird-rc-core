import { NextRequest, NextResponse } from 'next/server'
import { getCredential, revokeCredential } from '@/lib/server/clients/credentialsClient'
import { ApiError } from '@/lib/server/http'

export async function GET(_req: NextRequest, { params }: { params: { id: string } }) {
  try {
    return NextResponse.json(await getCredential(params.id))
  } catch (e) {
    if (e instanceof ApiError) return NextResponse.json({ message: e.message }, { status: e.status })
    throw e
  }
}

export async function DELETE(_req: NextRequest, { params }: { params: { id: string } }) {
  try {
    return NextResponse.json(await revokeCredential(params.id))
  } catch (e) {
    if (e instanceof ApiError) return NextResponse.json({ message: e.message }, { status: e.status })
    throw e
  }
}
