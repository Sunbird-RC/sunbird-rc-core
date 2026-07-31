import { NextRequest, NextResponse } from 'next/server'
import { getRegistrySchema, publishRegistrySchema } from '@/lib/server/clients/registryClient'
import { ApiError } from '@/lib/server/http'

export async function GET(_req: NextRequest, { params }: { params: { id: string } }) {
  try {
    return NextResponse.json(await getRegistrySchema(params.id))
  } catch (e) {
    if (e instanceof ApiError) return NextResponse.json({ message: e.message }, { status: e.status })
    throw e
  }
}

// Publish is the only lifecycle action this screen exposes today — it's a
// full PUT of the schema doc with status flipped, since the registry's
// generic controller does full-replace, not a lifecycle-specific endpoint.
export async function PUT(req: NextRequest, { params }: { params: { id: string } }) {
  const current = await req.json()
  try {
    return NextResponse.json(await publishRegistrySchema(params.id, current))
  } catch (e) {
    if (e instanceof ApiError) return NextResponse.json({ message: e.message }, { status: e.status })
    throw e
  }
}
