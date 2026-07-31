import { NextRequest, NextResponse } from 'next/server'
import { deleteDocument } from '@/lib/server/clients/registryClient'
import { ApiError } from '@/lib/server/http'

export async function DELETE(
  _req: NextRequest,
  { params }: { params: { entityType: string; entityId: string; property: string; docId: string } },
) {
  try {
    return NextResponse.json(await deleteDocument(params.entityType, params.entityId, params.property, params.docId))
  } catch (e) {
    if (e instanceof ApiError) return NextResponse.json({ message: e.message }, { status: e.status })
    throw e
  }
}
