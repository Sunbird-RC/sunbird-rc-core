import { NextRequest, NextResponse } from 'next/server'
import { transitionSchema, type LifecycleAction } from '@/lib/server/clients/credentialSchemaClient'
import { ApiError } from '@/lib/server/http'

const VALID: LifecycleAction[] = ['publish', 'deprecate', 'revoke']

export async function PUT(_req: NextRequest, { params }: { params: { action: string; id: string; version: string } }) {
  if (!VALID.includes(params.action as LifecycleAction)) {
    return NextResponse.json({ message: `Unknown lifecycle action: ${params.action}` }, { status: 400 })
  }
  try {
    return NextResponse.json(await transitionSchema(params.action as LifecycleAction, params.id, params.version))
  } catch (e) {
    if (e instanceof ApiError) return NextResponse.json({ message: e.message }, { status: e.status })
    throw e
  }
}
