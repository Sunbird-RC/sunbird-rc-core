import { NextRequest, NextResponse } from 'next/server'
import { createEntity, searchEntities } from '@/lib/server/clients/registryEntityClient'
import { ApiError } from '@/lib/server/http'

export async function GET(req: NextRequest, { params }: { params: { entityType: string } }) {
  const q = req.nextUrl.searchParams.get('q')
  const field = req.nextUrl.searchParams.get('field')
  // Collection's filter shape: { <field>: { <operator>: value } } — a
  // "contains" filter on one field when the operator caller supplies a
  // search box value, otherwise an unfiltered search (empty filters object).
  const filters = q && field ? { [field]: { contains: q } } : {}
  try {
    return NextResponse.json(await searchEntities(params.entityType, filters))
  } catch (e) {
    if (e instanceof ApiError) return NextResponse.json({ message: e.message }, { status: e.status })
    throw e
  }
}

export async function POST(req: NextRequest, { params }: { params: { entityType: string } }) {
  const body = await req.json()
  try {
    return NextResponse.json(await createEntity(params.entityType, body))
  } catch (e) {
    if (e instanceof ApiError) return NextResponse.json({ message: e.message }, { status: e.status })
    throw e
  }
}
