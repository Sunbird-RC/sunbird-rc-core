import { NextRequest, NextResponse } from 'next/server'
import { getSchemaVersion, saveSchemaVersion } from '@/lib/server/clients/credentialSchemaClient'
import { ApiError } from '@/lib/server/http'

export async function GET(_req: NextRequest, { params }: { params: { id: string; version: string } }) {
  try {
    return NextResponse.json(await getSchemaVersion(params.id, params.version))
  } catch (e) {
    if (e instanceof ApiError) return NextResponse.json({ message: e.message }, { status: e.status })
    throw e
  }
}

// Creates a NEW version — does not mutate the current row (verified backend
// behavior). Callers must separately hit the deprecate lifecycle action.
export async function PUT(req: NextRequest, { params }: { params: { id: string; version: string } }) {
  const body = await req.json()
  try {
    return NextResponse.json(await saveSchemaVersion(params.id, params.version, body))
  } catch (e) {
    if (e instanceof ApiError) return NextResponse.json({ message: e.message }, { status: e.status })
    throw e
  }
}
