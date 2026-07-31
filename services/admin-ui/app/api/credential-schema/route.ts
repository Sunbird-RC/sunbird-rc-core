import { NextRequest, NextResponse } from 'next/server'
import { createSchema, listSchemasByTag } from '@/lib/server/clients/credentialSchemaClient'
import { ApiError } from '@/lib/server/http'

export async function GET(req: NextRequest) {
  const tags = req.nextUrl.searchParams.get('tags')
  if (!tags) return NextResponse.json({ message: 'tags is required' }, { status: 400 })
  try {
    return NextResponse.json(await listSchemasByTag(tags))
  } catch (e) {
    if (e instanceof ApiError) return NextResponse.json({ message: e.message }, { status: e.status })
    throw e
  }
}

export async function POST(req: NextRequest) {
  const body = await req.json()
  try {
    return NextResponse.json(await createSchema(body))
  } catch (e) {
    if (e instanceof ApiError) return NextResponse.json({ message: e.message }, { status: e.status })
    throw e
  }
}
