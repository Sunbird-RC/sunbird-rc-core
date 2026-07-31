import { NextRequest, NextResponse } from 'next/server'
import { createTemplate, listTemplates } from '@/lib/server/clients/credentialSchemaClient'
import { ApiError } from '@/lib/server/http'

export async function GET(req: NextRequest) {
  const schemaId = req.nextUrl.searchParams.get('schemaId') ?? undefined
  try {
    return NextResponse.json(await listTemplates(schemaId))
  } catch (e) {
    if (e instanceof ApiError) return NextResponse.json({ message: e.message }, { status: e.status })
    throw e
  }
}

export async function POST(req: NextRequest) {
  const body = await req.json()
  try {
    return NextResponse.json(await createTemplate(body))
  } catch (e) {
    if (e instanceof ApiError) return NextResponse.json({ message: e.message }, { status: e.status })
    throw e
  }
}
