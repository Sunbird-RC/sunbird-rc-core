import { NextRequest, NextResponse } from 'next/server'
import { deleteTemplate, getTemplate } from '@/lib/server/clients/credentialSchemaClient'
import { ApiError } from '@/lib/server/http'

export async function GET(_req: NextRequest, { params }: { params: { id: string } }) {
  try {
    return NextResponse.json(await getTemplate(params.id))
  } catch (e) {
    if (e instanceof ApiError) return NextResponse.json({ message: e.message }, { status: e.status })
    throw e
  }
}

export async function DELETE(_req: NextRequest, { params }: { params: { id: string } }) {
  try {
    return NextResponse.json(await deleteTemplate(params.id))
  } catch (e) {
    if (e instanceof ApiError) return NextResponse.json({ message: e.message }, { status: e.status })
    throw e
  }
}
