import { NextRequest, NextResponse } from 'next/server'
import { getSchemaVersions } from '@/lib/server/clients/credentialSchemaClient'
import { ApiError } from '@/lib/server/http'

export async function GET(_req: NextRequest, { params }: { params: { id: string } }) {
  try {
    return NextResponse.json(await getSchemaVersions(params.id))
  } catch (e) {
    if (e instanceof ApiError) return NextResponse.json({ message: e.message }, { status: e.status })
    throw e
  }
}
