import { NextRequest, NextResponse } from 'next/server'
import { deleteEntity, getEntity, updateEntity } from '@/lib/server/clients/registryEntityClient'
import { ApiError } from '@/lib/server/http'

export async function GET(_req: NextRequest, { params }: { params: { entityType: string; id: string } }) {
  try {
    return NextResponse.json(await getEntity(params.entityType, params.id))
  } catch (e) {
    if (e instanceof ApiError) return NextResponse.json({ message: e.message }, { status: e.status })
    throw e
  }
}

export async function PUT(req: NextRequest, { params }: { params: { entityType: string; id: string } }) {
  const body = await req.json()
  try {
    return NextResponse.json(await updateEntity(params.entityType, params.id, body))
  } catch (e) {
    if (e instanceof ApiError) return NextResponse.json({ message: e.message }, { status: e.status })
    throw e
  }
}

export async function DELETE(_req: NextRequest, { params }: { params: { entityType: string; id: string } }) {
  try {
    return NextResponse.json(await deleteEntity(params.entityType, params.id))
  } catch (e) {
    if (e instanceof ApiError) return NextResponse.json({ message: e.message }, { status: e.status })
    throw e
  }
}
