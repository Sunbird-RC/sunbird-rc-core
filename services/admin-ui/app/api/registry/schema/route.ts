import { NextRequest, NextResponse } from 'next/server'
import { createRegistrySchema, listRegistrySchemas } from '@/lib/server/clients/registryClient'
import { ApiError } from '@/lib/server/http'

export async function GET() {
  try {
    return NextResponse.json(await listRegistrySchemas())
  } catch (e) {
    if (e instanceof ApiError) return NextResponse.json({ message: e.message }, { status: e.status })
    throw e
  }
}

export async function POST(req: NextRequest) {
  const body = await req.json()
  try {
    return NextResponse.json(await createRegistrySchema(body))
  } catch (e) {
    if (e instanceof ApiError) return NextResponse.json({ message: e.message }, { status: e.status })
    throw e
  }
}
